import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Child } from '../../../core/models/child.model';

/**
 * Tarjeta presentacional para el perfil de un niño.
 * Muestra el nombre, edad calculada y color del avatar.
 * Emite eventos para editar, eliminar y ver la agenda.
 */
@Component({
  selector: 'app-child-card',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './child-card.component.html',
  styleUrl: './child-card.component.scss'
})
export class ChildCardComponent {
  /** Perfil del niño a mostrar */
  @Input({ required: true }) child!: Child;

  /** Emitido cuando el padre quiere editar el perfil */
  @Output() edit        = new EventEmitter<Child>();
  /** Emitido cuando el padre quiere editar el avatar */
  @Output() editAvatar  = new EventEmitter<Child>();
  /** Emitido cuando el padre quiere eliminar el perfil */
  @Output() delete      = new EventEmitter<Child>();

  /** Calcula la edad en años a partir de la fecha de nacimiento */
  get age(): number {
    const birth = new Date(this.child.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }

  /** Retorna la primera letra del nombre en mayúscula para el avatar */
  get avatarLetter(): string {
    return this.child.name.charAt(0).toUpperCase();
  }
}
