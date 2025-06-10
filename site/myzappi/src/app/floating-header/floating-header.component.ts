import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';

@Component({
  selector: 'app-floating-header',
  templateUrl: './floating-header.component.html',
  styleUrls: ['./floating-header.component.css']
})
export class FloatingHeaderComponent implements OnInit {
  @Input() energyCost: string | null = 'N/A';
  @Input() costType: string = 'cost'; // Added input for cost type (cost or credit)
  @Input() logoUrl: string = 'assets/images/myzappi-icon-floating-bar.png'; // Updated to use local asset
  @Input() showEnergyCost: boolean = false; // Control whether to display energy cost section
  @Output() logoutClicked = new EventEmitter<void>();

  constructor() { }
  
  ngOnInit(): void {
    console.log('Logo URL:', this.logoUrl); // Debug logging
  }

  // Getter method to determine CSS class based on cost type
  get energyCostClass(): string {
    if (this.costType && this.costType.toLowerCase() === 'credit') {
      return 'energy-credit';
    } else {
      return 'energy-cost';
    }
  }

  onLogout(): void {
    this.logoutClicked.emit();
  }
}
