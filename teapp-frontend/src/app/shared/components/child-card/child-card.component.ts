import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Child } from '../../../core/models/child.model';

@Component({
  selector: 'app-child-card',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './child-card.component.html',
  styleUrl: './child-card.component.scss'
})
export class ChildCardComponent {
  @Input({ required: true }) child!: Child;

  @Output() edit        = new EventEmitter<Child>();
  @Output() editAvatar  = new EventEmitter<Child>();
  @Output() delete      = new EventEmitter<Child>();

  get age(): number {
    const birth = new Date(this.child.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }

  get avatarLetter(): string {
    return this.child.name.charAt(0).toUpperCase();
  }
}
