import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { SuccessDialogComponent } from './success-dialog.component';

describe('SuccessDialogComponent', () => {
  let component: SuccessDialogComponent;
  let fixture: ComponentFixture<SuccessDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SuccessDialogComponent],  // Declare the component
      providers: [
        { provide: MatDialogRef, useValue: {} },  // Provide MatDialogRef
        { provide: MAT_DIALOG_DATA, useValue: {} }  // Provide MAT_DIALOG_DATA
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SuccessDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
