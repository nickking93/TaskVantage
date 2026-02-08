import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'app-help-page',
    imports: [
        CommonModule,
        MatTabsModule,
        MatCardModule,
        MatIconModule,
        MatButtonModule,
        MatExpansionModule,
        RouterModule
    ],
    templateUrl: './help-page.component.html',
    styleUrl: './help-page.component.css'
})
export class HelpPageComponent {

}
