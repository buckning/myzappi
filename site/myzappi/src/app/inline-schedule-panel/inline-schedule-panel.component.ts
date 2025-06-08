import { Component, ComponentFactoryResolver, EventEmitter, Input, OnInit, Output, ViewChild, ViewContainerRef, Inject } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ScheduleActionComponent } from '../schedule-action.interface';
import { Schedule } from '../schedule.interface';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SchedulerService } from '../scheduler.service';
import { ScheduleDialogComponent } from './schedule-dialog/schedule-dialog.component';

@Component({
  selector: 'app-inline-schedule-panel',
  templateUrl: './inline-schedule-panel.component.html',
  styleUrls: ['./inline-schedule-panel.component.css']
})
export class InlineSchedulePanelComponent implements OnInit {
  @Input() actionComponentType: any;
  @Input() public bearerToken: any;
  @Input() serialNumber: any;
  @Input() deviceName: string = '';

  constructor(
    private componentFactoryResolver: ComponentFactoryResolver,
    private http: HttpClient,
    private schedulerService: SchedulerService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {}

  openScheduleDialog() {
    const dialogRef = this.dialog.open(ScheduleDialogComponent, {
      width: '450px',
      disableClose: true,
      data: {
        actionComponentType: this.actionComponentType,
        bearerToken: this.bearerToken,
        serialNumber: this.serialNumber,
        deviceName: this.deviceName,
        componentFactoryResolver: this.componentFactoryResolver
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('The schedule dialog was closed with result:', result);
    });
  }
}
