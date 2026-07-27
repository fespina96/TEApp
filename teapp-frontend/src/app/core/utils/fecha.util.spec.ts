import { fechaISOLocal } from './fecha.util';

describe('fechaISOLocal', () => {

  it('devuelve la fecha local, no la UTC, en horario nocturno', () => {
    // 27/07/2026 22:30 en horario local: en husos negativos (Argentina, UTC-3)
    // toISOString() devolvería el día siguiente.
    const noche = new Date(2026, 6, 27, 22, 30, 0);

    expect(fechaISOLocal(noche)).toBe('2026-07-27');
  });

  it('rellena mes y día con cero a la izquierda', () => {
    expect(fechaISOLocal(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('sin argumentos usa la fecha de hoy en horario local', () => {
    const hoy = new Date();
    const esperado = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;

    expect(fechaISOLocal()).toBe(esperado);
  });

  it('coincide con la fecha local aunque el momento sea fin de mes por la noche', () => {
    const finDeMes = new Date(2026, 6, 31, 23, 59, 0);

    expect(fechaISOLocal(finDeMes)).toBe('2026-07-31');
  });
});
