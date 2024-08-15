import { Component } from '@angular/core';

@Component({
  selector: 'app-data-deletion',
  standalone: true,
  templateUrl: './data-deletion.component.html',
  styleUrls: ['./data-deletion.component.css']
})
export class DataDeletionComponent {

  deleteUser() {
    // Implement the logic to delete the user data from your backend
    // You can make an HTTP request to your backend API to delete the user data
  }

}
