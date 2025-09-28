import { Component, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChatMessageResponse, ChatWs, CreateChatMessageRequest, RtcSignal } from '../../core/chat-core';

// ===== DTOs sesji =====
type MessageType = 'TEXT' | 'FILE' | 'SYSTEM' | 'SIGNALING';

interface CreateGuestSessionRequest { email: string; }
interface GuestSessionResponse {
  sessionId: string;
  email: string;
  verified: boolean;
  createdAt: string;
  verificationToken?: string | null;
  captchaA?: number | null;
  captchaB?: number | null;
}
interface VerifyGuestSessionRequest {
  sessionId: string;
  verificationToken: string;
  captchaResult: number;
}

// ===== WebRTC sygnalizacja (dopasuj do backendu) =====
const RTC_OFFER_DEST     = (targetSid: string) => `/app/rtc/${targetSid}/offer`;
const RTC_ANSWER_DEST    = (targetSid: string) => `/app/rtc/${targetSid}/answer`;
const RTC_CANDIDATE_DEST = (targetSid: string) => `/app/rtc/${targetSid}/candidate`;
const RTC_BYE_DEST       = (targetSid: string) => `/app/rtc/${targetSid}/bye`;

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css'],
})
export class ChatComponent implements OnDestroy {
  // === ENDPOINTY REST ===
  private readonly sessionApi = '/api-session';
  private readonly apiBase = '/api-chat';

  // === UI state ===
  email = '';
  token = '';
  captcha: number | null = null;
  captchaA: number | null = null;
  captchaB: number | null = null;

  roomId = '';
  sender = '';
  input = '';

  sessionId: string | null = null;
  verified = false;

  messages: ChatMessageResponse[] = [];
  wsConnected = false;
  info = '';

  // === Typing (UI) ===
  typingPeers: string[] = [];
  private typingTimers = new Map<string, any>();
  private typingSelfDebounce?: any;

  // === WebRTC ===
  calleeSessionId = '';
  private pc?: RTCPeerConnection;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private pttStream: MediaStream | null = null;

  // najlepiej pobrać z backendu; demo:
  private iceServers: RTCIceServer[] = [
    { urls: ['stun:stun.l.google.com:19302'] },
    // { urls: ['turn:YOUR_IP:3478?transport=udp'], username: 'livecomm', credential: 'superSecretPass' },
    // { urls: ['turn:YOUR_IP:3478?transport=tcp'], username: 'livecomm', credential: 'superSecretPass' },
  ];

  constructor(private http: HttpClient, private chatWs: ChatWs) {}

  ngOnDestroy(): void {
    this.disconnectWs();
    this.tearDownRtc();
  }

  // ======== computed ========
  get joinDisabled(): boolean {
    return !this.verified || !this.roomId.trim() || !this.sender.trim();
  }

  // ======== SESJA ========
  createSession(): void {
    const email = this.email.trim();
    if (!email) { this.info = 'Podaj e-mail.'; return; }

    const body: CreateGuestSessionRequest = { email };
    this.http.post<GuestSessionResponse>(`${this.sessionApi}/api/guest-session`, body).subscribe({
      next: (res) => {
        this.sessionId = res.sessionId;
        this.verified = res.verified;
        this.token = res.verificationToken ?? '';
        this.captchaA = res.captchaA ?? null;
        this.captchaB = res.captchaB ?? null;
        this.info = 'Sesja utworzona. Przepisz token i wynik captcha, potem kliknij Verify.';
      },
      error: () => this.info = 'Błąd tworzenia sesji.',
    });
  }

  verify(): void {
    if (!this.sessionId) { this.info = 'Brak sessionId – najpierw utwórz sesję.'; return; }
    if (!this.token.trim()) { this.info = 'Podaj token weryfikacyjny.'; return; }
    if (this.captcha === null || isNaN(Number(this.captcha))) { this.info = 'Podaj poprawny wynik captcha.'; return; }

    const body: VerifyGuestSessionRequest = {
      sessionId: this.sessionId,
      verificationToken: this.token.trim(),
      captchaResult: Number(this.captcha),
    };
    this.http.post<GuestSessionResponse>(`${this.sessionApi}/api/guest-session/verify`, body).subscribe({
      next: (res) => {
        this.verified = res.verified;
        this.token = ''; this.captcha = null; this.captchaA = null; this.captchaB = null;
        this.info = 'Sesja zweryfikowana. Możesz dołączyć do pokoju.';
      },
      error: () => this.info = 'Błąd weryfikacji sesji.',
    });
  }

  // ======== CZAT: join + history + WS ========
  async joinRoom(): Promise<void> {
    const rid = this.roomId.trim();
    if (this.joinDisabled || !this.sessionId) return;

    const afterConnect = async () => {
      try {
        // presence + ping + suby
        this.chatWs.publish(`/app/rooms/${rid}/presence/join`, '');
        this.chatWs.ping();

        this.subscribeRoom(rid);
        this.subscribeTyping(rid);
        this.subscribeRtcInbox();

        this.messages = await this.chatWs.loadHistory(this.apiBase, rid);
        queueMicrotask(() => document.getElementById('messagesEnd')?.scrollIntoView({ behavior: 'smooth' }));
        this.info = 'Dołączono do pokoju.';
      } catch (e) {
        console.error('joinRoom failed', e);
        this.info = 'Nie udało się dołączyć do pokoju.';
      }
    };

    if (!this.chatWs.connected) {
      this.chatWs.ensureConnected(this.sessionId!, {
        onConnect: () => { this.wsConnected = true; afterConnect(); },
        onDisconnect: () => { this.wsConnected = false; },
        onError: () => { this.info = 'STOMP error – sprawdź logi backendu.'; },
      });
    } else {
      await afterConnect();
    }
  }

  private subscribeRoom(roomId: string): void {
    try {
      this.chatWs.subscribeRoom(roomId, (msg) => {
        this.messages = [...this.messages, msg];
        queueMicrotask(() => document.getElementById('messagesEnd')?.scrollIntoView({ behavior: 'smooth' }));
      });
    } catch (e) {
      console.error('subscribe error', e);
      this.info = 'Błąd subskrypcji pokoju.';
    }
  }


  private subscribeTyping(roomId: string): void {
    try {
      this.chatWs.subscribeTyping(roomId, (ev: { roomId: string; sessionId: string; typing: boolean; ts: string }) => {
        if (!ev?.sessionId || ev.sessionId === this.sessionId) return;
        const nick = ev.sessionId;

        if (ev.typing) {

          if (!this.typingPeers.includes(nick)) this.typingPeers = [...this.typingPeers, nick];
          clearTimeout(this.typingTimers.get(nick));
          const t = setTimeout(() => {
            this.typingPeers = this.typingPeers.filter(n => n !== nick);
            this.typingTimers.delete(nick);
          }, 2500);
          this.typingTimers.set(nick, t);
        } else {

          this.typingPeers = this.typingPeers.filter(n => n !== nick);
          clearTimeout(this.typingTimers.get(nick));
          this.typingTimers.delete(nick);
        }
      });
    } catch {}
  }


  onTypingKey(): void {
    const rid = this.roomId.trim();
    if (!rid) return;


    this.chatWs.sendTyping(rid, true);


    clearTimeout(this.typingSelfDebounce);
    this.typingSelfDebounce = setTimeout(() => {
      this.chatWs.sendTyping(rid, false);
    }, 1200);
  }

  disconnectWs(): void {
    this.chatWs.disconnect();
    this.wsConnected = false;
    // wyczyść typujących
    this.typingPeers = [];
    this.typingTimers.forEach(t => clearTimeout(t));
    this.typingTimers.clear();
  }

  // ======== SEND ========
  send(): void {
    if (!this.sessionId || !this.verified) { this.info = 'Brak zweryfikowanej sesji.'; return; }
    const rid = this.roomId.trim();
    const nick = this.sender.trim();
    const text = this.input.trim();
    if (!rid || !nick || !text) return;

    const dto: CreateChatMessageRequest = {
      sessionId: this.sessionId,
      sender: nick,
      type: 'TEXT' as MessageType,
      content: text,
      roomId: rid,
    };

    try {
      if (this.chatWs.connected) {
        this.chatWs.sendMessage(rid, dto);
        this.input = '';
        this.chatWs.sendTyping(rid, false);
        return;
      }
    } catch {}
    this.http.post<ChatMessageResponse>(`${this.apiBase}/api/chat/messages`, dto).subscribe({
      next: (saved) => { this.messages = [...this.messages, saved]; this.input = ''; },
      error: () => this.info = 'Nie udało się wysłać wiadomości.',
    });
  }

  // ======================================================================
  // ===========================  WEBRTC  ==================================
  // ======================================================================

  private get localVideo(): HTMLVideoElement {
    return document.getElementById('localVideo') as HTMLVideoElement;
  }
  private get remoteVideo(): HTMLVideoElement {
    return document.getElementById('remoteVideo') as HTMLVideoElement;
  }
  private get remoteAudio(): HTMLAudioElement {
    return document.getElementById('remoteAudio') as HTMLAudioElement;
  }

  private DEFAULT_AUDIO: MediaTrackConstraints = {
    echoCancellation: true, noiseSuppression: true, autoGainControl: true,
  };
  private DEFAULT_VIDEO: MediaTrackConstraints = {
    width: { ideal: 1280, max: 1280 }, height: { ideal: 720, max: 720 },
    frameRate: { ideal: 30, max: 30 }, facingMode: 'user',
  };

  private stopTracks(stream?: MediaStream | null) {
    stream?.getTracks().forEach(t => t.stop());
  }

  private async pickDevices() {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const cams = devices.filter(d => d.kind === 'videoinput');
    const mics = devices.filter(d => d.kind === 'audioinput');
    return { videoDeviceId: cams[0]?.deviceId, audioDeviceId: mics[0]?.deviceId };
  }

  private async ensureLocalStream(): Promise<MediaStream> {
    this.stopTracks(this.localStream);

    const { videoDeviceId, audioDeviceId } = await this.pickDevices();

    const c1: MediaStreamConstraints = {
      audio: { ...this.DEFAULT_AUDIO, deviceId: audioDeviceId ? { exact: audioDeviceId } : undefined },
      video: { ...this.DEFAULT_VIDEO, deviceId: videoDeviceId ? { exact: videoDeviceId } : undefined },
    };
    const c2: MediaStreamConstraints = {
      audio: { ...this.DEFAULT_AUDIO },
      video: { width: { ideal: 640 }, height: { ideal: 360 }, frameRate: { max: 30 } },
    };
    const c3: MediaStreamConstraints = { audio: { ...this.DEFAULT_AUDIO }, video: false };

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia(c1);
      return this.localStream;
    } catch (e1: any) {
      console.warn('getUserMedia(c1) failed', e1?.name || e1);
      try {
        this.localStream = await navigator.mediaDevices.getUserMedia(c2);
        return this.localStream;
      } catch (e2: any) {
        console.warn('getUserMedia(c2) failed', e2?.name || e2);
        this.localStream = await navigator.mediaDevices.getUserMedia(c3);
        return this.localStream;
      }
    }
  }

  private attachLocal(stream: MediaStream) {
    const el = this.localVideo;
    el.srcObject = stream;
    el.muted = true;
    el.playsInline = true;
    el.autoplay = true;
    el.play().catch(() => {});
  }

  private attachRemote(stream: MediaStream) {
    const v = this.remoteVideo;
    const a = this.remoteAudio;
    v.srcObject = stream; v.playsInline = true; v.autoplay = true; v.play().catch(() => {});
    a.srcObject = stream; a.autoplay = true; a.play().catch(() => {});
  }

  private wireOnTrack(pc: RTCPeerConnection) {
    pc.ontrack = (ev) => {
      if (!this.remoteStream) this.remoteStream = new MediaStream();
      const track = ev.track;
      if (!this.remoteStream.getTracks().some(t => t.id === track.id)) {
        this.remoteStream.addTrack(track);
      }
      this.attachRemote(this.remoteStream);
    };
  }

  private async createPeerIfNeeded() {
    if (this.pc) return;
    this.pc = new RTCPeerConnection({ iceServers: this.iceServers });
    this.wireOnTrack(this.pc);


    this.pc.onicecandidate = (ev) => {
      if (ev.candidate && this.calleeSessionId.trim() && this.sessionId) {
        const target = this.calleeSessionId.trim();
        const candidate = typeof ev.candidate.toJSON === 'function' ? ev.candidate.toJSON() : ev.candidate;
        const payload = {
          type: 'candidate',
          candidate,
          fromSessionId: this.sessionId,
          toSessionId: target,
          roomId: this.roomId?.trim() || undefined,
        };
        this.chatWs.publish(RTC_CANDIDATE_DEST(target), JSON.stringify(payload), {
          'content-type': 'application/json',
        });
      }
    };
  }

  async onStartCall(): Promise<void> {
    try {
      if (!this.sessionId) { this.info = 'Brak sessionId'; return; }
      if (!this.calleeSessionId.trim()) { this.info = 'Podaj sessionId rozmówcy (demo)'; return; }

      await this.createPeerIfNeeded();

      const stream = await this.ensureLocalStream();
      stream.getTracks().forEach(t => this.pc!.addTrack(t, stream));
      this.attachLocal(stream);

      const offer = await this.pc!.createOffer({ offerToReceiveAudio: true, offerToReceiveVideo: true });
      await this.pc!.setLocalDescription(offer);

      const target = this.calleeSessionId.trim();
      this.calleeSessionId = target;
      const payload = {
        type: 'offer',
        sdp: offer.sdp,
        fromSessionId: this.sessionId,
        toSessionId: target,
        roomId: this.roomId?.trim() || undefined,
      };
      this.chatWs.publish(RTC_OFFER_DEST(target), JSON.stringify(payload), {
        'content-type': 'application/json',
      });

      this.info = 'Wysłano offer.';
    } catch (e: any) {
      console.error(e);
      this.info = e?.message || 'Start call failed';
    }
  }

  async onEndCall(): Promise<void> {
    if (this.sessionId && this.calleeSessionId.trim()) {
      const target = this.calleeSessionId.trim();
      const payload = {
        type: 'bye',
        fromSessionId: this.sessionId,
        toSessionId: target,
        roomId: this.roomId?.trim() || undefined,
      };
      this.chatWs.publish(RTC_BYE_DEST(target), JSON.stringify(payload), {
        'content-type': 'application/json',
      });
    }
    this.tearDownRtc();
    this.info = 'Połączenie zakończone.';
  }

  private tearDownRtc() {
    try { this.pc?.getSenders().forEach(s => s.track?.stop()); } catch {}
    this.stopTracks(this.localStream);
    this.stopTracks(this.pttStream);
    this.localStream = null;
    this.pttStream = null;
    this.remoteStream = null;

    if (this.localVideo) this.localVideo.srcObject = null;
    if (this.remoteVideo) this.remoteVideo.srcObject = null;
    if (this.remoteAudio) this.remoteAudio.srcObject = null;

    try { this.pc?.close(); } catch {}
    this.pc = undefined;
  }


  private async onRtcSignal(sig: RtcSignal) {
    if (!this.sessionId || sig?.toSessionId !== this.sessionId) return;

    if (!this.pc) await this.createPeerIfNeeded();

    if (sig?.type === 'offer' && sig?.sdp) {

      const stream = await this.ensureLocalStream();
      stream.getTracks().forEach(t => this.pc!.addTrack(t, stream));
      this.attachLocal(stream);

      await this.pc!.setRemoteDescription({ type: 'offer', sdp: sig.sdp });
      const answer = await this.pc!.createAnswer();
      await this.pc!.setLocalDescription(answer);

      if (sig.fromSessionId) {
        const payload = {
          type: 'answer',
          sdp: answer.sdp,
          fromSessionId: this.sessionId,
          toSessionId: sig.fromSessionId,
          roomId: this.roomId?.trim() || undefined,
        };
        this.calleeSessionId = sig.fromSessionId;
        this.chatWs.publish(RTC_ANSWER_DEST(sig.fromSessionId), JSON.stringify(payload), {
          'content-type': 'application/json',
        });
      }
    } else if (sig?.type === 'answer' && sig?.sdp) {
      this.calleeSessionId = sig.fromSessionId ?? this.calleeSessionId;
      await this.pc!.setRemoteDescription({ type: 'answer', sdp: sig.sdp });
    } else if (sig?.type === 'candidate' && sig?.candidate) {
      this.calleeSessionId = sig.fromSessionId ?? this.calleeSessionId;
      try { await this.pc!.addIceCandidate(sig.candidate); } catch (e) { console.warn('addIceCandidate error', e); }
    } else if (sig?.type === 'bye') {
      this.calleeSessionId = '';
      this.tearDownRtc();
      this.info = 'Rozmówca zakończył połączenie.';
    } else {
      console.warn('Unknown RTC signal', sig);
    }
  }


  private subscribeRtcInbox(): void {
    try {
      this.chatWs.subscribeRtcInbox((sig) => this.onRtcSignal(sig));
    } catch {}
  }


  async pttDown(): Promise<void> {
    try {
      if (this.pttStream) return;
      this.pttStream = await navigator.mediaDevices.getUserMedia({ audio: this.DEFAULT_AUDIO, video: false });
      if (this.pc) {
        this.pttStream.getAudioTracks().forEach(t => this.pc!.addTrack(t, this.pttStream!));
      }
      this.info = 'PTT: mic on';
    } catch (e: any) {
      console.warn('PTT mic error', e);
      this.info = 'PTT: brak dostępu do mikrofonu';
    }
  }

  pttUp(): void {
    if (!this.pttStream) return;
    this.pttStream.getTracks().forEach(t => t.stop());
    this.pttStream = null;
    this.info = 'PTT: mic off';
  }
}
