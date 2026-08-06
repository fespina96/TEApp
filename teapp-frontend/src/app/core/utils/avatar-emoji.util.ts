/**
 * Los avatares del catálogo se guardan como un SVG en data URI: un círculo de
 * color con un emoji encima. El color vive dentro del SVG, así que para cambiar
 * el fondo hay que volver a generarlo.
 *
 * El formato tiene que coincidir con el de AvatarEmoji.java en Android: lo que
 * se elige en una plataforma se ve en la otra.
 */

const PREFIJO = 'data:image/svg+xml;base64,';

const COLOR = /<circle[^>]*fill="([^"]+)"/;
const TEXTO = /<text[^>]*>([^<]+)<\/text>/;

/** Las dos piezas que definen un avatar del catálogo. */
export interface AvatarEmoji {
  emoji: string;
  color: string;
}

/** @returns true si el avatar es del catálogo y no una foto subida */
export function esAvatarDelCatalogo(avatar?: string | null): boolean {
  return !!avatar && avatar.startsWith(PREFIJO);
}

/**
 * @returns el emoji y el color, o null si no es un avatar del catálogo o el
 *          contenido no tiene la forma esperada
 */
export function leerAvatarEmoji(avatar?: string | null): AvatarEmoji | null {
  if (!esAvatarDelCatalogo(avatar)) return null;
  try {
    const svg = decodeURIComponent(escape(atob(avatar!.slice(PREFIJO.length))));
    const color = COLOR.exec(svg);
    const texto = TEXTO.exec(svg);
    if (!color || !texto) return null;

    const emoji = texto[1].trim();
    return emoji ? { emoji, color: color[1] } : null;
  } catch {
    return null;
  }
}

export function avatarEmojiADataUrl(emoji: string, color: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">`
    + `<circle cx="50" cy="50" r="50" fill="${color}"/>`
    + `<text x="50" y="68" font-size="52" text-anchor="middle">${emoji}</text>`
    + `</svg>`;
  return PREFIJO + btoa(unescape(encodeURIComponent(svg)));
}

/**
 * Devuelve el avatar con otro color de fondo. Una foto subida se devuelve tal
 * cual: el color del perfil solo pinta el círculo que hay detrás.
 */
export function recolorearAvatar(avatar: string | undefined, color: string): string | undefined {
  const delCatalogo = leerAvatarEmoji(avatar);
  return delCatalogo ? avatarEmojiADataUrl(delCatalogo.emoji, color) : avatar;
}
