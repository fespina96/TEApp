import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { connectivityInterceptor } from './connectivity.interceptor';
import { ConnectivityService } from '../services/connectivity.service';
import { environment } from '../../../environments/environment';

describe('connectivityInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let connectivitySpy: jasmine.SpyObj<ConnectivityService>;

  const apiUrl      = `${environment.apiUrl}/children`;
  const externalUrl = 'https://api.arasaac.org/v1/pictograms/es/search?q=casa';

  beforeEach(() => {
    connectivitySpy = jasmine.createSpyObj('ConnectivityService', ['markDisconnected']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([connectivityInterceptor])),
        provideHttpClientTesting(),
        { provide: ConnectivityService, useValue: connectivitySpy }
      ]
    });

    http     = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // URLs externas

  it('URL externa: petición exitosa → pasa sin tocar ConnectivityService', () => {
    http.get(externalUrl).subscribe();
    httpMock.expectOne(externalUrl).flush([]);

    expect(connectivitySpy.markDisconnected).not.toHaveBeenCalled();
  });

  it('URL externa: error de red (status 0) → NO llama a markDisconnected', () => {
    http.get(externalUrl).subscribe({ error: () => {} });
    httpMock.expectOne(externalUrl).error(new ProgressEvent('network'), { status: 0 });

    expect(connectivitySpy.markDisconnected).not.toHaveBeenCalled();
  });

  // URLs de la API

  it('URL de API con respuesta exitosa: no llama a markDisconnected', () => {
    http.get(apiUrl).subscribe();
    httpMock.expectOne(apiUrl).flush([]);

    expect(connectivitySpy.markDisconnected).not.toHaveBeenCalled();
  });

  it('URL de API con status 0: llama a markDisconnected', () => {
    http.get(apiUrl).subscribe({ error: () => {} });
    httpMock.expectOne(apiUrl).error(new ProgressEvent('network'), { status: 0 });

    expect(connectivitySpy.markDisconnected).toHaveBeenCalledTimes(1);
  });

  it('URL de API con status 401: NO llama a markDisconnected', () => {
    http.get(apiUrl).subscribe({ error: () => {} });
    httpMock.expectOne(apiUrl).error(new ProgressEvent('error'), { status: 401 });

    expect(connectivitySpy.markDisconnected).not.toHaveBeenCalled();
  });

  it('URL de API con status 500: NO llama a markDisconnected', () => {
    http.get(apiUrl).subscribe({ error: () => {} });
    httpMock.expectOne(apiUrl).error(new ProgressEvent('error'), { status: 500 });

    expect(connectivitySpy.markDisconnected).not.toHaveBeenCalled();
  });
});
