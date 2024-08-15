import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing'; // Import RouterTestingModule
import { HomeComponent } from './home.component';
import { ActivatedRoute } from '@angular/router'; // Import ActivatedRoute
import { of } from 'rxjs'; // Import of to mock observables

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        HomeComponent // Import HomeComponent as a standalone component
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: '1' }), 
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
