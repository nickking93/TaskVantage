import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { FirebaseMessagingService } from './firebase-messaging.service';

describe('FirebaseMessagingService', () => {
  let service: FirebaseMessagingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(FirebaseMessagingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
