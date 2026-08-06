import {
  avatarEmojiADataUrl,
  esAvatarDelCatalogo,
  leerAvatarEmoji,
  recolorearAvatar
} from './avatar-emoji.util';

describe('avatar-emoji.util', () => {

  const FOTO = 'data:image/jpeg;base64,/9j/4AAQSkZJRg==';

  it('recupera el emoji y el color de un avatar que acaba de generar', () => {
    const url = avatarEmojiADataUrl('🦋', '#C9B8E8');

    expect(leerAvatarEmoji(url)).toEqual({ emoji: '🦋', color: '#C9B8E8' });
  });

  it('lee los avatares guardados con el SVG en varias líneas', () => {
    // Formato que generaba la versión anterior del selector; sigue en la base.
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
      <circle cx="50" cy="50" r="50" fill="#FAF0BE"/>
      <text x="50" y="68" font-size="52" text-anchor="middle">⭐</text>
    </svg>`;
    const url = `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`;

    expect(leerAvatarEmoji(url)).toEqual({ emoji: '⭐', color: '#FAF0BE' });
  });

  it('cambia el color de fondo conservando el emoji', () => {
    const original = avatarEmojiADataUrl('🚀', '#A8D8EA');

    expect(leerAvatarEmoji(recolorearAvatar(original, '#B8E0C8'))).toEqual({
      emoji: '🚀',
      color: '#B8E0C8'
    });
  });

  it('deja intacta una foto subida: el color solo pinta el círculo de atrás', () => {
    expect(recolorearAvatar(FOTO, '#B8E0C8')).toBe(FOTO);
    expect(leerAvatarEmoji(FOTO)).toBeNull();
    expect(esAvatarDelCatalogo(FOTO)).toBeFalse();
  });

  it('no falla cuando no hay avatar', () => {
    expect(leerAvatarEmoji(undefined)).toBeNull();
    expect(recolorearAvatar(undefined, '#B8E0C8')).toBeUndefined();
  });

  it('devuelve null si el contenido no es el SVG esperado', () => {
    const roto = `data:image/svg+xml;base64,${btoa('<svg></svg>')}`;

    expect(leerAvatarEmoji(roto)).toBeNull();
  });
});
