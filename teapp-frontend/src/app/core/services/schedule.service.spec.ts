import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ScheduleService } from './schedule.service';
import { WeeklySchedule } from '../models/schedule-entry.model';
import { environment } from '../../../environments/environment';

describe('ScheduleService', () => {
  let service: ScheduleService;
  let httpMock: HttpTestingController;

  const childId = 'child-uuid-123';
  const entryId = 'entry-uuid-456';
  const today   = new Date().toISOString().split('T')[0];

  const mockSchedule: WeeklySchedule = {
    childId: childId,
    week: {
      MONDAY:    { MORNING: [], AFTERNOON: [], NIGHT: [] },
      TUESDAY:   { MORNING: [], AFTERNOON: [], NIGHT: [] },
      WEDNESDAY: { MORNING: [], AFTERNOON: [], NIGHT: [] },
      THURSDAY:  { MORNING: [], AFTERNOON: [], NIGHT: [] },
      FRIDAY:    { MORNING: [], AFTERNOON: [], NIGHT: [] },
      SATURDAY:  { MORNING: [], AFTERNOON: [], NIGHT: [] },
      SUNDAY:    { MORNING: [], AFTERNOON: [], NIGHT: [] }
    }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ScheduleService]
    });
    service = TestBed.inject(ScheduleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // getWeeklySchedule

  it('getWeeklySchedule: debe hacer GET sin parámetros', () => {
    service.getWeeklySchedule(childId).subscribe(schedule => {
      expect(schedule).toEqual(mockSchedule);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/children/${childId}/schedule`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSchedule);
  });

  it('getWeeklySchedule: con filtro de día → incluye query param', () => {
    service.getWeeklySchedule(childId, 'MONDAY').subscribe();

    const req = httpMock.expectOne(r => r.url.includes(`/children/${childId}/schedule`));
    expect(req.request.params.get('day')).toBe('MONDAY');
    req.flush(mockSchedule);
  });

  // markCompleted

  it('markCompleted: debe hacer POST con la fecha', () => {
    service.markCompleted(childId, entryId, today).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiUrl}/children/${childId}/schedule/${entryId}/completions`
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ date: today });
    req.flush(null);
  });

  // unmarkCompleted

  it('unmarkCompleted: debe hacer DELETE con la fecha en el body', () => {
    service.unmarkCompleted(childId, entryId, today).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiUrl}/children/${childId}/schedule/${entryId}/completions`
    );
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual({ date: today });
    req.flush(null);
  });

  // resetCurrentWeek

  it('resetCurrentWeek: debe hacer DELETE al endpoint de la semana', () => {
    service.resetCurrentWeek(childId).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiUrl}/children/${childId}/completions/current-week`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // addEntry / deleteEntry

  it('addEntry: debe hacer POST con los datos de la entrada', () => {
    const entryReq = {
      activityId: 'act-uuid', dayOfWeek: 'MONDAY' as const,
      timeSlot: 'MORNING' as const, sortOrder: 0
    };
    service.addEntry(childId, entryReq).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/children/${childId}/schedule`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(entryReq);
    req.flush({});
  });

  it('deleteEntry: debe hacer DELETE al endpoint de la entrada', () => {
    service.deleteEntry(childId, entryId).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiUrl}/children/${childId}/schedule/${entryId}`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
