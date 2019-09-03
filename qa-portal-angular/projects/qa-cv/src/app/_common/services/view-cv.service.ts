import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { ICvModel } from '../models/qac-cv-db.model';
import { catchError, map, tap } from 'rxjs/operators';
import { MessageService } from './message.service';

import * as _ from 'lodash';
import { GET_ALL_CVS, POST_CV_DATA } from '../models/cv.constants';

@Injectable({ providedIn: 'root' })
export class ViewCvService {



  httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(
    private http: HttpClient,
    private messageService: MessageService) { }





 

  /** GET cv by id. Will 404 if id not found */
  getLatestCvForCurrentUser(): Observable<ICvModel> {
    return this.getAllCvsForCurrentUser().pipe(
      map(allCvs => _.head(_.orderBy(allCvs, ['versionNumber'], ['desc'])))
    );
  }

  private getAllCvsForCurrentUser(): Observable<ICvModel[]> {
    return this.http.get<ICvModel[]>(GET_ALL_CVS, this.httpOptions).pipe(
      catchError(this.handleError<ICvModel[]>(`getICvModel for current user`))
    );

  }

  getPDF(cv: ICvModel){
    const url = `cv-api/cv/generated`;
    const httpOptions = {
      'responseType'  : 'arraybuffer' as 'json'

    };
  
    return this.http.post<any>(url,cv,httpOptions);
    
    }

  //////// Save methods //////////

  // /** POST: add a new cv to the server */
  // addCv(cv: ICvModel): Observable<ICvModel> {
  //   return this.http.post<ICvModel>(POST_CV_DATA, cv, this.httpOptions).pipe(
  //     tap((newICvModel: ICvModel) => this.log(`added cv w/ id=${newICvModel.id}`)),
  //     catchError(this.handleError<ICvModel>('addICvModel'))
  //   );
  // }

  /** PUT: update the cv on the server */
  updateCv(cv: ICvModel): Observable<ICvModel> {
    return this.http.put(POST_CV_DATA, cv, this.httpOptions).pipe(
      tap(_ => this.log(`updated cv id=${cv.id}`)),
      catchError(this.handleError<any>('updateICvModel'))
    );
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {

      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead

      // TODO: better job of transforming error for user consumption
      this.log(`${operation} failed: ${error.message}`);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }

  /** Log a ViewCvService message with the MessageService */
  private log(message: string) {
    this.messageService.add(`ViewCvService: ${message}`);
  }
}