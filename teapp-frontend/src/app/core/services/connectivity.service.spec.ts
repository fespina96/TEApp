import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ConnectivityService } from './connectivity.service';
import { environment } from '../../../environments/environment';

describe('ConnectivityService', () => {
  let service: ConnectivityService;
  let httpMock: HttpTestingController;

  const healthUrl = `${environment.apiUrl}/health`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ConnectivityService]
    });
    service  = TestBed.inject(ConnectivityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ─── estado inicial ──────────────────────────────────────────────────────

  it('estado inicial: connected$ emite true', (done) => {
    service.connected$.subscribe(val => {
      expect(val).toBeTrue();
      done();
    });
  });

  it('isConnected: devuelve true por defecto', () => {
    expect(service.isConnected).toBeTrue();
  });

  // ─── check() ─────────────────────────────────────────────────────────────

  it('check(): servidor responde OK → connected$ permanece en true', () => {
    service.check();
    httpMock.expectOne(healthUrl).flush({ status: 'ok' });

    service.connected$.subscribe(val => expect(val).toBeTrue());
  });

  it('check(): error de red (status 0) → connected$ emite false', () => {
    let emitted: boolean | undefined;
    service.connected$.subscribe(val => emitted = val);

    service.check();
    httpMock.expectOne(healthUrl).error(new ProgressEvent('network'), { status: 0 });

    expect(emitted).toBeFalse();
  });

  it('check(): error HTTP (status 401) → connected$ NO cambia a false', () => {
    let emitted: boolean | undefined;
    service.connected$.subscribe(val => emitted = val);

    service.check();
    httpMock.expectOne(healthUrl).error(new ProgressEvent('error'), { status: 401 });

    expect(emitted).toBeTrue();
  });

  it('check(): segunda llamada mientras verifica → se ignora (una sola petición)', () => {
    service.check();
    service.check();

    // Solo debe haber una petición HTTP
    httpMock.expectOne(healthUrl).flush({ status: 'ok' });
    httpMock.verify();
  });

  it('check(): puede volver a llamarse después de que termina la primera', () => {
    service.check();
    httpMock.expectOne(healthUrl).flush({ status: 'ok' });

    service.check();
    httpMock.expectOne(healthUrl).flush({ status: 'ok' });
  });

  // ─── markDisconnected() ───────────────────────────────────────────────────

  it('markDisconnected(): connected$ emite false', () => {
    let emitted: boolean | undefined;
    service.connected$.subscribe(val => emitted = val);

    service.markDisconnected();

    expect(emitted).toBeFalse();
    expect(service.isConnected).toBeFalse();
  });
});
