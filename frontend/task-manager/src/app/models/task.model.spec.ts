import { Task } from './task.model';

describe('Task', () => {
  it('should create an instance', () => {
    const task = new Task(
      'Sample Task',           // title
      'This is a sample task', // description
      'High',                  // priority
      false,                   // recurring
      '2024-08-15'             // dueDate
    );
    expect(task).toBeTruthy();
  });
});
