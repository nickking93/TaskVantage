import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HomeComponent } from './home.component';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TaskService } from '../services/task.service';
import { MatDialog } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let authServiceMock: any;
  let taskServiceMock: any;
  let matDialogMock: any;

  beforeEach(async () => {
    authServiceMock = jasmine.createSpyObj('AuthService', ['getUserDetails', 'logout']);
    authServiceMock.getUserDetails.and.returnValue(of({ id: '1', username: 'testuser' }));
    authServiceMock.logout.and.returnValue(of({}));

    taskServiceMock = jasmine.createSpyObj('TaskService', ['getTaskSummary', 'createTask']);
    taskServiceMock.getTaskSummary.and.returnValue(of({
      totalTasks: 5,
      totalSubtasks: 3,
      pastDeadlineTasks: 2,
      completedTasksThisMonth: 4,
      totalTasksThisMonth: 7
    }));
    taskServiceMock.createTask.and.returnValue(of({}));

    matDialogMock = jasmine.createSpyObj('MatDialog', ['open']);
    matDialogMock.open.and.returnValue({ afterClosed: () => of({}) });

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        BrowserAnimationsModule,
        HomeComponent
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: TaskService, useValue: taskServiceMock },
        { provide: MatDialog, useValue: matDialogMock },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({ get: (key: string) => '1' }),
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Additional tests can be added here to test other aspects of the component
});
