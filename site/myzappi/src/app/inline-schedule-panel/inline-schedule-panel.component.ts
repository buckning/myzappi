import { Component, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SchedulerService } from '../scheduler.service';
import { ScheduleDialogComponent } from './schedule-dialog/schedule-dialog.component';

@Component({
    selector: 'app-inline-schedule-panel',
    templateUrl: './inline-schedule-panel.component.html',
    styleUrls: ['./inline-schedule-panel.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class InlineSchedulePanelComponent implements OnInit {
  @Input() actionComponentType: any;
  @Input() public bearerToken: any;
  @Input() serialNumber: any;
  @Input() deviceName: string = '';

  constructor(
    private http: HttpClient,
    private schedulerService: SchedulerService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {}

  openScheduleDialog() {
    const dialogRef = this.dialog.open(ScheduleDialogComponent, {
      width: '90%',
      maxWidth: '450px',
      disableClose: true,
      autoFocus: false,
      data: {
        actionComponentType: this.actionComponentType,
        bearerToken: this.bearerToken,
        serialNumber: this.serialNumber,
        deviceName: this.deviceName
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('The schedule dialog was closed with result:', result);
    });
  }
}
