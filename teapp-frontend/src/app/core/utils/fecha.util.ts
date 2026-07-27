/**
 * Devuelve la fecha en formato ISO (AAAA-MM-DD) usando el huso horario local.
 *
 * No se usa toISOString() porque convierte a UTC: en Argentina (UTC-3) toda
 * actividad marcada después de las 21:00 quedaría registrada con la fecha del
 * día siguiente, mientras que Android y el backend usan la fecha local.
 */
export function fechaISOLocal(fecha: Date = new Date()): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}
