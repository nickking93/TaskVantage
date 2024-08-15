import { Task } from './task.model';

describe('Task', () => {
  it('should create an instance', () => {
    const task = new Task(
      'Sample Task',           
      'This is a sample task', 
      '2024-08-15',            
      'High',                  
      false                    
    );
    expect(task).toBeTruthy();
  });
});
