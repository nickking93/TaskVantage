import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';  // Import HttpClientTestingModule
import { RouterTestingModule } from '@angular/router/testing';           // Import RouterTestingModule if needed
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';  // Import AuthService or other required services

describe('AuthGuard', () => {
  let guard: AuthGuard;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],  // Import necessary modules
      providers: [AuthGuard, AuthService]  // Provide AuthGuard and AuthService
    });
    guard = TestBed.inject(AuthGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });
});
