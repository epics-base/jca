/**********************************************************************

 *

 *      Original Author: Eric Boucher

 *      Date:            05/05/2003

 *

 *      Experimental Physics and Industrial Control System (EPICS)

 *

 *      Copyright 1991, the University of Chicago Board of Governors.

 *

 *      This software was produced under  U.S. Government contract

 *      W-31-109-ENG-38 at Argonne National Laboratory.

 *

 *      Beamline Controls & Data Acquisition Group

 *      Experimental Facilities Division

 *      Advanced Photon Source

 *      Argonne National Laboratory

 *

 *

 * $Id: TimeStamp.java,v 1.2 2004-08-12 07:24:05 msekoranja Exp $

 *

 * Modification Log:

 * 01. 05/07/2003  erb  initial development

 *

 */



package gov.aps.jca.dbr;



import java.math.BigDecimal;

import java.util.Calendar;

import java.util.Date;







public class TimeStamp {



  public TimeStamp() {

    long millis= System.currentTimeMillis();

    secPastEpoch= millis/1000 - TS_EPOCH_SEC_PAST_1970;

    nsec= (millis%1000)*1000000;

  }



  public TimeStamp(long secPastEpoch, long nsec) {

    this.secPastEpoch= secPastEpoch;

    this.nsec= nsec;

  }



  public TimeStamp(double dbl) {

    secPastEpoch= (long) dbl;

    nsec= (long)((dbl - (double)secPastEpoch)*1000000000.0);

  }



  public TimeStamp(TimeStamp ts) {

    secPastEpoch= ts.secPastEpoch;

    nsec= ts.nsec;

  }



  public void add(double dbl) {

    long secPastEpoch,nsec;

    if (dbl>=0.) {

      secPastEpoch= (long)dbl;

      nsec= (long) ((dbl-(double)secPastEpoch)*1000000000.0);

      add(secPastEpoch, nsec);

    } else {

      secPastEpoch= (long)-dbl;

      nsec= (long)((-dbl-(double)secPastEpoch)*1000000000.0);

      sub(secPastEpoch, nsec);

    }

  }



  public void add(TimeStamp ts) {

    add(ts.secPastEpoch, ts.nsec);

  }



  public void sub(double dbl) {

    add(-dbl);

  }



  public void sub(TimeStamp ts) {

    sub(ts.secPastEpoch, ts.nsec);

  }





  public int cmp(TimeStamp ts) {

    if (secPastEpoch<ts.secPastEpoch) return -1;

    else if (secPastEpoch==ts.secPastEpoch) {

      if (nsec<ts.nsec) return -1;

      else if (nsec==ts.nsec) return 0;

      else return 1;

    } else return 1;

  }



  public boolean EQ(TimeStamp ts) {

    return((secPastEpoch==ts.secPastEpoch) && (nsec==ts.nsec));

  }



  public boolean NE(TimeStamp ts) {

    return((secPastEpoch!=ts.secPastEpoch) || (nsec!=ts.nsec));

  }



  public boolean GT(TimeStamp ts) {

    if (secPastEpoch>ts.secPastEpoch) return true;

    return((secPastEpoch==ts.secPastEpoch) && (nsec>ts.nsec));

  }



  public boolean GE(TimeStamp ts) {

    if (secPastEpoch>ts.secPastEpoch) return true;

    return((secPastEpoch==ts.secPastEpoch) && (nsec>=ts.nsec));

  }



  public boolean LT(TimeStamp ts) {

    if (secPastEpoch<ts.secPastEpoch) return true;

    return((secPastEpoch==ts.secPastEpoch) && (nsec<ts.nsec));

  }



  public boolean LE(TimeStamp ts) {

    if (secPastEpoch<ts.secPastEpoch) return true;

    return((secPastEpoch==ts.secPastEpoch) && (nsec<=ts.nsec));

  }



  public double asDouble() {

    return((double)secPastEpoch + (double)nsec/1000000000.0);

  }





  /**

   * Get the timestamp in seconds since the epoch as a BigDecimal preserving

   * the precision of the input timestamp. This method was added by Tom Pelaia.

   * @return timestamp in seconds since the epoch preserving the full precision available

   */

  public BigDecimal asBigDecimal() {

    final BigDecimal baseTime = BigDecimal.valueOf(secPastEpoch);

    final BigDecimal nanoTime = BigDecimal.valueOf(nsec);



    BigDecimal fraction = nanoTime.multiply(nanoScale);



    return baseTime.add(fraction);

  }





  public String toMMDDYY() {

    int month;

    int day;

    int year;

    int hours;

    int minutes;

    int seconds;

    Date date= new Date((secPastEpoch+TS_EPOCH_SEC_PAST_1970)*1000+nsec/1000000);

    Calendar cal= Calendar.getInstance();

    cal.setTime(date);



    StringBuffer buff= new StringBuffer(40);

    String fracPart= format(9, nsec);

    if (nsec % 1000000 == 0)   fracPart= fracPart.substring(0,3);

    else if (nsec % 1000 == 0) fracPart= fracPart.substring(0,6);





    if (secPastEpoch<=86400) {

      hours  = (int) (secPastEpoch / 3600);

      minutes= (int) ((secPastEpoch / 60) % 60);

      seconds= (int) (secPastEpoch % 60);

    } else {

      month= cal.get(Calendar.MONTH)+1;

      day= cal.get(Calendar.DAY_OF_MONTH);

      year= cal.get(Calendar.YEAR) % 100 ;

      hours= cal.get(Calendar.HOUR_OF_DAY);

      minutes= cal.get(Calendar.MINUTE);

      seconds= cal.get(Calendar.SECOND);

      buff.append(format(2,month));

      buff.append(":");

      buff.append(format(2,day));

      buff.append(":");

      buff.append(format(2,year));

      buff.append(" ");

    }

    buff.append(format(2,hours));

    buff.append(":");

    buff.append(format(2,minutes));

    buff.append(":");

    buff.append(format(2,seconds));

    buff.append(".");

    buff.append(fracPart);



    return buff.toString();

  }



  public String toMONDDYYYY() {

    int month;

    int day;

    int year;

    int hours;

    int minutes;

    int seconds;

    Date date= new Date((secPastEpoch+TS_EPOCH_SEC_PAST_1970)*1000+nsec/1000000);

    Calendar cal= Calendar.getInstance();

    cal.setTime(date);



    StringBuffer buff= new StringBuffer(40);

    String fracPart= format(9, nsec);

    if (nsec % 1000000 == 0)   fracPart= fracPart.substring(0,3);

    else if (nsec % 1000 == 0) fracPart= fracPart.substring(0,6);





    if (secPastEpoch<=86400) {

      hours  = (int) (secPastEpoch / 3600);

      minutes= (int) ((secPastEpoch / 60) % 60);

      seconds= (int) (secPastEpoch % 60);

    } else {

      month= cal.get(Calendar.MONTH);

      day= cal.get(Calendar.DAY_OF_MONTH);

      year= cal.get(Calendar.YEAR);

      hours= cal.get(Calendar.HOUR_OF_DAY);

      minutes= cal.get(Calendar.MINUTE);

      seconds= cal.get(Calendar.SECOND);

      buff.append(monthText[month]);

      buff.append(" ");

      buff.append(format(2,day));

      buff.append(",");

      buff.append(format(2,year));

      buff.append(" ");

    }

    buff.append(format(2,hours));

    buff.append(":");

    buff.append(format(2,minutes));

    buff.append(":");

    buff.append(format(2,seconds));

    buff.append(".");

    buff.append(fracPart);



    return buff.toString();

  }







  public static TimeStamp add(TimeStamp ts, double dbl) {

    TimeStamp res= new TimeStamp(ts);

    res.add(dbl);

    return res;

  }



  public static TimeStamp add(TimeStamp ts1, TimeStamp ts2) {

    TimeStamp res= new TimeStamp(ts1);

    res.add(ts2);

    return res;

  }



  public static TimeStamp sub(TimeStamp ts, double dbl) {

    TimeStamp res= new TimeStamp(ts);

    res.sub(dbl);

    return res;

  }



  public static TimeStamp sub(TimeStamp ts1, TimeStamp ts2) {

    TimeStamp res= new TimeStamp(ts1);

    res.sub(ts2);

    return res;

  }





//  public static void main(String[] args) {
//
//    TimeStamp ts= new TimeStamp(86500, 0);
//
//    System.out.println(ts.toMONDDYYYY());
//
//    System.out.println(ts.toMMDDYY());
//
//    ts= new TimeStamp();
//
//    System.out.println(ts.toMONDDYYYY());
//
//    System.out.println(ts.toMMDDYY());
//
//  }







  private void add(long secPastEpoch, long nsec) {

    this.secPastEpoch+= secPastEpoch;

    if ((this.nsec+= nsec)>=1000000000) {

      this.secPastEpoch+= this.nsec/1000000000;

      this.nsec%= 1000000000;

    }

  }



  private void sub(long secPastEpoch, long nsec) {

    this.secPastEpoch-= secPastEpoch;

    if (this.nsec>=nsec) {

      this.nsec-=nsec;

    } else {

      this.nsec+=1000000000-nsec;

      --this.secPastEpoch;

    }

  }



  private String format(int width, int value) {

    StringBuffer buff= new StringBuffer();

    buff.append(value);

    while (buff.length()<width) buff.insert(0, "0");

    return buff.toString();

  }



  private String format(int width, long value) {

    StringBuffer buff= new StringBuffer();

    buff.append(value);

    while (buff.length()<width) buff.insert(0, "0");

    return buff.toString();

  }



  public long secPastEpoch() { return secPastEpoch; }

  public long nsec() { return nsec; }







  private long secPastEpoch;

  private long nsec;



  //private static long TS_EPOCH_YEAR         =1990;

  private static long TS_EPOCH_SEC_PAST_1970=7305*86400;

  //private static long TS_EPOCH_WDAY_NUM     =1;



  final private static BigDecimal nanoScale = new BigDecimal(1.0e-9);



  private static String[] monthText= { "Jan", "Feb", "Mar", "Apr",

    "May", "Jun", "Jul", "Aug",

    "Sep", "Oct", "Nov", "Dec"};

};















