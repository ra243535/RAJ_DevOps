package custom.app.workorder;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import psdi.app.calendar.WorkPeriodRemote;
import psdi.app.common.DateUtility;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.mbo.SqlFormat;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class TQWOExt extends WOExt {
	static boolean savedAll = false;
	private MXLogger myLogger;
	public MboRemote createWorkorder(String jpnum) throws MXException,
			RemoteException {
		MboSet workorderSet = (MboSet) getMboServer().getMboSet("WORKORDER",
				getUserInfo());
		workorderSet.setInsertSite(getString("siteid"));
		workorderSet.setInsertOrg(getString("orgid"));
		MboRemote workorderMbo = workorderSet.add();
		UserInfo userInfo = workorderMbo.getUserInfo();
		userInfo.setInteractive(false);
		workorderMbo.setValue("origrecordid", getString("wonum"), 11L);
		workorderMbo.setValue("origrecordclass", getString("woclass"), 11L);
		workorderMbo.setValue("origwoid", getString("wonum"), 2L);

		workorderMbo.setValue("WORKTYPE", "CM", 2L);

		if (!(workorderMbo.isNull("origrecordid")))
			workorderMbo.setValue("reportedby", userInfo.getPersonId(), 2L);
		try {
			workorderMbo.setValue("classstructureid",
					getString("classstructureid"), 2L);
		} catch (Exception localException) {
		}

		setValue("hasfollowupwork", true, 2L);

		if ((!(workorderMbo.getString("assetnum").equalsIgnoreCase("")))
				&& (!(workorderMbo.getString("location").equalsIgnoreCase("")))) {
			if (!(getString("assetnum").equalsIgnoreCase(workorderMbo
					.getString("assetnum")))) {
				workorderMbo.setValue("assetnum", getString("assetnum"), 2L);
			}
			if (!(getString("location").equalsIgnoreCase(workorderMbo
					.getString("location")))) {
				workorderMbo.setValue("location", getString("location"), 2L);
			}
		}
		if ((jpnum != null) && (!(jpnum.equals("")))) {
			workorderMbo.setValue("jpnum", jpnum, 2L);
		}
		userInfo.setInteractive(true);

		MboSetRemote relatedrecset = workorderMbo.getMboSet("RELATEDRECORD");
		MboRemote relatedrec = relatedrecset.add();
		relatedrec.setValue("relatedreckey", getString("wonum"), 11L);
		relatedrec.setValue("relatedrecclass", getString("woclass"), 11L);
		relatedrec.setValue("relatedrecsiteid", getString("siteid"), 11L);
		relatedrec.setValue("relatedrecorgid", getString("orgid"), 11L);
		relatedrec.setValue("relatetype",
				getTranslator().toExternalDefaultValue("RELATETYPE",
						"ORIGINATOR", relatedrec), 11L);
		MboSetRemote multiAssetSet = getMboSet("MULTIASSETLOCCI");
		multiAssetSet.copy(workorderMbo.getMboSet("MULTIASSETLOCCI"));
		MboSetRemote mypmultiSetRemote = getMboSet("LINEARRELATED");

		mypmultiSetRemote.copy(workorderMbo.getMboSet("LINEARRELATED"));
		workorderSet.save();

		String theWOClassDesc = ((TQWOExt) workorderMbo)
				.getWOClassDescription(workorderMbo);
		Object[] parms = { workorderMbo.getString("wonum"), theWOClassDesc };
		((MboSet) getThisMboSet()).addWarning(new MXApplicationException(
				"workorder", "WorkorderCreated", parms));

		return workorderMbo;
	}

	public TQWOExt(MboSet ms) throws MXException, RemoteException {
		super(ms);
		this.myLogger = MXLoggerFactory.getLogger("maximo.merge");
	}

	public String getWOClassDescription(MboRemote theMboRemote)
			throws MXException, RemoteException {
		String tempClassDesc = "workorder";
		try {
			String tempClass = getTranslator().toInternalString("WOCLASS",
					theMboRemote.getString("woclass"));
			SqlFormat sql = new SqlFormat(this,
					" domainid='WOCLASS' and maxvalue=:1 ");
			sql.setObject(1, "synonymdomain", "maxvalue", tempClass);
			MboRemote theRemote = getMboSet("$sysnonymdomain", "synonymdomain",
					sql.format()).getMbo(0);
			if (theRemote != null)
				tempClassDesc = theRemote.getString("description");
		} catch (Throwable localThrowable) {
		}
		return tempClassDesc;
	}

	public void save() throws MXException, RemoteException {
		System.out.println("");
		if ((getMboValue("STATUS").isModified())
				&& (getString("STATUS").equalsIgnoreCase("COMP"))) {
			String failurecode = getString("FAILURECODE");
			if (!(failurecode.equalsIgnoreCase(""))) {
				MboSetRemote FRSet = getMboSet("FAILUREREPORT");
				FRSet.reset();

				String problem = "";
				String type = "";

				for (int j = 0; j < FRSet.count(); ++j) {
					type = FRSet.getMbo(j).getString("TYPE");
					if (type.equalsIgnoreCase("PROBLEM"))
						problem = FRSet.getMbo(j).getString("FAILURECODE");
				}
				if (problem.equalsIgnoreCase("")) {
					FRSet.add().setValue("FAILURECODE", "NOF");
					FRSet.add().setValue("FAILURECODE", "NOBFC");
					FRSet.add().setValue("FAILURECODE", "NOF");
				}

			}

		}
		Calendar setOrigTargDate = Calendar.getInstance();
		if ((getString("WORKTYPE").equalsIgnoreCase("PM"))
				&& (getString("STATUS").equalsIgnoreCase("WSCH"))
				&& (getString("TQTARGSTARTDATE").equalsIgnoreCase(""))
				&& (!(getString("TARGSTARTDATE").equalsIgnoreCase("")))) {
			setOrigTargDate.setTime(getDate("TARGSTARTDATE"));
			getMboValue("TQTARGSTARTDATE").setValue(setOrigTargDate.getTime(),
					11L);
		}
		
		if ((getMboValue("STATUS").isModified())
				&& (!(getString("WORKTYPE").equalsIgnoreCase("PM")))
				&& (!(getString("STATUS").equalsIgnoreCase("DFWAPPR")))
				&& (!(getString("STATUS").equalsIgnoreCase("OIMACK")))
				&& (!(getString("STATUS").equalsIgnoreCase("WAPPR")))
				&& (!(getString("STATUS").equalsIgnoreCase("WPREP")))
				&& (getString("TQTARGSTARTDATE").equalsIgnoreCase(""))
				&& (!(getString("TARGSTARTDATE").equalsIgnoreCase("")))) {
			setOrigTargDate.setTime(getDate("TARGSTARTDATE"));
			getMboValue("TQTARGSTARTDATE").setValue(setOrigTargDate.getTime(),
					11L); 
		}
		
		
		/*
		 * Santosh - PRB0012313 - Duplicate work order changes
		 * when follow up work order is created all p6 fields should be cleared
		 * */
		if (!(hasCustomization())) {
			this.myLogger.info("Org does not have access, calling super");
			super.save();
			return;
		} 
		 
		if (this.isNew()) {
			if (this.toBeAdded()){  
	                String origrecordid = this.getString("ORIGRECORDID");
	                String origrecordclass = "";
	                origrecordclass = this.getString("ORIGRECORDCLASS");
	                if(origrecordid != null && origrecordclass != ""){
	                	  populateTemp(this);
	                      if (origrecordclass.equals("WORKORDER") || origrecordclass.equals("ACTIVITY")){
								this.setValueNull("P628GATESTAT", 11L); 
								this.setValueNull("P684GATESTAT", 11L);	
								this.setValueNull("P6AREACOMM", 11L);
								this.setValueNull("P6GANTTCHART", 11L);
								this.setValueNull("P6OSJOBOFF", 11L);
								this.setValueNull("P6OSJOBSPON", 11L); 
								this.setValueNull("P6POB", 11L);
								this.setValueNull("P6PLANSCHED", 11L);
								this.setValueNull("P6SITEJOBOFF", 11L);
								this.setValueNull("P6TASKID", 11L);
								this.setValueNull("P6VENDORNAME", 11L);
								this.setValueNull("P6CALID", 11L); 
								this.setValueNull("TQPLANNINGNOTES", 11L);
								this.setValueNull("WOJP5", 11L);
								this.setValue("TQPSF", false, 2L); 
								
								/* Change By - Sayali
								 * Date - 17/07/2017
								 * Change - PRB0011869 Stop PM worktype for non PM work orders
								 * for more details check problem ticket 
								 * */ 
								this.setValueNull("TQPMNUM", 11L);
								
								
								// Sayali- PRB0011869-Stop PM worktype for non PM work orders - End of Code
								
								/*
								 * Change By - Santosh 
								 * Change - PRB0012427 Work Order Review flag changes
								 * */
								this.setValue("TQCOMPFLAG", false, 2L);  
								// Santosh- PRB0012427- Work Order Review flag changes - End of Code

	                     }
	                }
			}
		}
		
		// Santosh - PRB0012313 - Duplicate work order changes - end of code
		

		
		/* Change By - Sayali
		 * Date - 19/07/2017
		 * Change - PRB0011869 Stop PM worktype for non PM work orders
		 * for more details check problem ticket 
		 * */ 
	
  
	
		if(this.toBeSaved()){
		  //String worktype=this.getString("WORKTYPE");
		  String parent = this.getString("PARENT");
		
        
			
			if (this.getString("WORKTYPE").equalsIgnoreCase("PM")&& this.getString("WOCLASS").equalsIgnoreCase("WORKORDER") && (parent != ""))
			{
		      
			  MboSetRemote parentset = this.getMboSet("PARENT"); 
			  			  
			  if (parentset.count() >0){
				    this.setValue("TQPMNUM", parentset.getMbo(0).getString("PMNUM"), 2L); 
				  }			 
			}
			else if(this.getString("WORKTYPE").equalsIgnoreCase("PM") && this.getString("WOCLASS").equalsIgnoreCase("WORKORDER") && (parent == ""))
			{
				this.setValue("TQPMNUM",this.getString("PMNUM"), 2L); 
			}
		}
		
		
		// Sayali- PRB0011869-Stop PM worktype for non PM work orders - End of Code	
		

	/* Change By - Reshma  
	* Date - 05/07/2017
	* Change - PRB11901 Non-PM WO Target Start and End Date
	* for more details check problem ticket 
	* */ 

	
	
	// Reshma - PRB11901 Non-PM WO Target Start and End Date - End of Code 

				
		/*

		  Populate the WORKORDER.DESCRIPTION long-description  with the following template 
		  *Change By - Rajendra sahu
		  * Date - 27/07/2017
		  * Change - PRB0011899 UK WO ENHANCEMENT - Templating of Long Description text for non-PM WOs  (CR018)
		  * for more details check problem ticket

	*/ 
			
		     if(getMboValue("JPNUM").isModified() && (!getMboValue("JPNUM").isNull() && this.getString("WORKTYPE") != "PM") && this.getString("ROUTE") == "" ){
		   	  
		  	   MboSetRemote jpset = this.getMboSet("JOBPLAN");
		  		if (jpset.count() > 0 ){
		  			if (!(jpset.getMbo(0).getString("DESCRIPTION_LONGDESCRIPTION") == null)){
		  				this.setValue("DESCRIPTION_LONGDESCRIPTION", jpset.getMbo(0).getString("DESCRIPTION_LONGDESCRIPTION"), 2L);
		  				
		  			}					
		  		}				
		  	  }
			  
			/*
			 * Change By - Santosh 
			 * Change - PRB0012427 Work Order Review flag changes
			* */
		if(getMboValue("PARENT").isModified()){                
			//if parent is null flag will be set to null
			if(this.getString("PARENT") == "" ){
				this.setValue("TQCOMPFLAG", false,11L);
			}
			else  {
				//if parent flag is not null
				if(this.getString("WOCLASS").equalsIgnoreCase("WORKORDER")){
					MboRemote parent = this.getMboSet("PARENT").getMbo(0);
					MboSetRemote worklogset = this.getMboSet("MODIFYWORKLOG");
					MboSetRemote parentworklogset = parent.getMboSet("MODIFYWORKLOG"); 
					
					//parent has a log and child doesn't
					if ((parentworklogset.count() > 0) && (worklogset.count() == 0)){
						this.setValue("TQCOMPFLAG", false,2l);
					}
					
					//child has a log but parent doesn't 
					if ((parentworklogset.count() == 0) && (worklogset.count() > 0)){
						this.setValue("TQCOMPFLAG", true); 
					}
					
					//parent and child both have logs
					if (worklogset.count() > 0 &&  parentworklogset.count()>0){
							worklogset.setOrderBy("WORKLOGID");
							parentworklogset.setOrderBy("WORKLOGID");
							worklogset.reset();
							parentworklogset.reset();
						//parent and child both have logs - child logs are more than parent logs
						if (parentworklogset.count() < worklogset.count()){
							this.setValue("TQCOMPFLAG", true);
							}
							
						//parent and child both have logs - child logs are equal and same as parent logs 	
						else if (parentworklogset.count() >= worklogset.count()){
							for (int i=0; i<worklogset.count(); i++){
								MboRemote log = worklogset.getMbo(i);
								for (int j=0; j<parentworklogset.count(); j++){
									MboRemote plog = parentworklogset.getMbo(j);
									if(plog.getString("LOGTYPE").equalsIgnoreCase(log.getString("LOGTYPE"))
									&& plog.getString("DESCRIPTION_LONGDESCRIPTION").equalsIgnoreCase(log.getString("DESCRIPTION_LONGDESCRIPTION"))
									&& plog.getString("CREATEBY").equalsIgnoreCase(log.getString("CREATEBY"))) {
									this.setValue("TQCOMPFLAG", false);
									break;
										}
							//parent and child both have logs - child logs are equal and different from parent logs 
									else {
										this.setValue("TQCOMPFLAG", true);	
									}
								}
							}									
						}
						
						
						
					}
				}
			}
		}
		// Santosh- PRB0012427- Work Order Review flag changes - End of Code
		
		if(this.toBeAdded() && this.getString("WORKTYPE").equalsIgnoreCase("PM")){
			this.setValue("SCHEDSTART", this.getDate("TARGSTARTDATE"),11L);
			this.setValue("SCHEDFINISH", this.getDate("TARGCOMPDATE"),11L);
		}	
		if(this.toBeAdded() && this.getString("TQDEVIATIONCOUNT") == ""){
			this.setValue("TQDEVIATIONCOUNT", 0,11L);
		}
		
		MboSetRemote apprStatusSet = this.getMboSet("TQWOSTATUS");
		apprStatusSet.setWhere("status not in ('WAPPR') ");
		apprStatusSet.reset();
		
		if(apprStatusSet.getMbo(0) != null ){
			if(!this.toBeAdded() && this.toBeSaved() && getMboValue("STATUS").isModified() 
					&& getMboValue("Status").getPreviousValue().toString().equalsIgnoreCase("WAPPR") ){
				this.setValue("TQDEVIATIONCOUNT", this.getInt("TQDEVIATIONCOUNT")+1,11L);	
			}
			apprStatusSet.close();
		}
		
		if(getMboValue("STATUS").isModified() && getMboValue("Status").getPreviousValue().toString().equalsIgnoreCase("WAPPR")
				&& this.getString("TARGSTARTDATE") == ""){
			
			populateTDs(this.getInt("WOPRIORITY"),this.getDouble("ESTDUR"));
			populateSDs(this.getInt("WOPRIORITY"),this.getDouble("ESTDUR"));
		}
		
		super.save();
		}
	// Rajendra - PRB0011899 UK WO ENHANCEMENT - Templating of Long Description text for non-PM WOs  (CR018) - End of Code
	/*

	  Populate the WORKORDER.DESCRIPTION long-description  with the following template 
	  *Change By - Rajendra sahu
	  * Date - 27/07/2017
	  * Change - PRB0011899 UK WO ENHANCEMENT - Templating of Long Description text for non-PM WOs  (CR018)
	  * for more details check problem ticket

*/ 
	
 public void add() throws MXException, RemoteException {
		
		super.add();
		if (!(hasCustomization())) 
		{
			this.myLogger.info("Org does not have access, calling super");
			return;
			
		}
		populateTemp(this);
	
  }

//Rajendra -  PRB0011899 UK WO ENHANCEMENT - Templating of Long Description text for non-PM WOs  (CR018) - End of Code 

	public void init() throws MXException {
		super.init();
		try {
			getMboValue("TQDEVIATIONCOUNT").setReadOnly(true);
		} catch (Exception Ex) {
			Ex.printStackTrace();
		}
	}
	
			/*
			 * Change By - Santosh 
			 * Change - PRB0012427 Work Order Review flag changes
			* */
	public void updateReviewFlag(boolean flag) throws MXException, RemoteException {
		this.setValue("TQCOMPFLAG", flag, 11L);
		 
	}
	// Santosh- PRB0012427- Work Order Review flag changes - End of Code
	
	public MboRemote duplicate() throws MXException, RemoteException {
		MboRemote duplicateRecord = super.duplicate();
		duplicateRecord.setValueNull("TQTARGSTARTDATE", 2L);
		duplicateRecord.setValueNull("TQDEVIATIONCOUNT", 2L);
		
		/*
		 * Santosh - PRB0012313 - Duplicate work order changes
		 * when work order is duplicating all p6 fields should be cleared
		 * */
		
		/*

		  Populate the WORKORDER.DESCRIPTION long-description  with the following template 
		  *Change By - Rajendra sahu
		  * Date - 17/07/2017
		  * when work order is duplicating then long-description and justification field should be cleared.
		  * Change - PRB0011899 UK WO ENHANCEMENT - Templating of Long Description text for non-PM WOs  (CR018)
		  * for more details check problem ticket.
 
	*/
		duplicateRecord.setValueNull("P628GATESTAT", 2L);
		duplicateRecord.setValueNull("P684GATESTAT", 2L);	
		duplicateRecord.setValueNull("P6AREACOMM", 2L);
		duplicateRecord.setValueNull("P6GANTTCHART", 2L);
		duplicateRecord.setValueNull("P6OSJOBOFF", 2L);
		duplicateRecord.setValueNull("P6OSJOBSPON", 2L);
		duplicateRecord.setValueNull("P6POB", 2L);
		duplicateRecord.setValueNull("P6PLANSCHED", 2L);
		duplicateRecord.setValueNull("P6SITEJOBOFF", 2L);
		duplicateRecord.setValueNull("P6TASKID", 2L);
		duplicateRecord.setValueNull("P6VENDORNAME", 2L);
		duplicateRecord.setValueNull("P6CALID", 2L);
		duplicateRecord.setValueNull("TQPLANNINGNOTES", 2L);
		duplicateRecord.setValueNull("WOJP5", 2L);
		//duplicateRecord.setValueNull("TQPSF", 2L);
		duplicateRecord.setValue("TQPSF", false, 2L);
		
		// Santosh - PRB0012313 - Duplicate work order changes - end of code

		
		/* Change By - Sayali
		 * Date - 17/07/2017
		 * Change - PRB0011869 Stop PM worktype for non PM work orders
		 * for more details check problem ticket 
		 * */ 
	
		duplicateRecord.setValueNull("TQPMNUM", 11L);

		// Sayali- PRB0011869-Stop PM worktype for non PM work orders - End of Code	
		
		/*
		* Change By - Santosh 
		* Change - PRB0012427 Work Order Review flag changes
		* */
		duplicateRecord.setValue("TQCOMPFLAG", false, 2L);
		// Santosh- PRB0012427- Work Order Review flag changes - End of Code
		
		if (!(hasCustomization())) 
		{
			this.myLogger.info("Org does not have access, calling super");
			return duplicateRecord;
			
		}
		populateTemp(duplicateRecord); 

		return duplicateRecord;
	}
	
public void populateTDs(int priority, double dur) throws MXException, RemoteException {
		

		/* Change By - Reshma  
		* Date - 05/07/2017
		* Change - PRB11901 Non-PM WO Target Start and End Date
		* for more details check problem ticket 
		* */ 

				
		int severity;
		String lof;
		int tqdaydiff;
				
		severity = this.getInt("SEVERITY");  
		lof = this.getString("LIKEOFOCC");
		boolean relatedflag = false; 
		java.util.Date firstStatusDate = new java.util.Date();
		
		//String internalStatus = getTranslator().toInternalString("WOSTATUS", this.getString("STATUS"));		 

		MboSetRemote woStatusSet = this.getMboSet("TQWOSTATUS");//("$CalSet", "WOSTATUS", (new StringBuilder("status != 'WAPPR' and wonum = '")).append(this.getString("WONUM")).append("' ").toString());
		woStatusSet.setWhere("status not in ('WAPPR') ");
		woStatusSet.setOrderBy("changedate asc");
		woStatusSet.reset(); 
		
		if( !getString("WORKTYPE").equalsIgnoreCase("PM") && (woStatusSet.getMbo(0)!= null )
				|| (getMboValue("STATUS").isModified() && this.getString("TARGSTARTDATE") == "" 
					&& getMboValue("STATUS").getPreviousValue().toString().equalsIgnoreCase("WAPPR") )){
			
			if(woStatusSet.getMbo(0)!= null){
				firstStatusDate = woStatusSet.getMbo(0).getDate("CHANGEDATE"); 	
			}
			 					
			MboSetRemote relatedrecset = this.getMboSet("RELATEDRECORD");  
			if (relatedrecset.count() > 0 ){
				 
				for (int r = 0; r<relatedrecset.count();r++){
					
					String relStatus = getTranslator().toInternalString("WOSTATUS", relatedrecset.getMbo(r).getString("RELATEDRECWO.STATUS"));
					String reltype = relatedrecset.getMbo(r).getString("RELATEDRECWOCLASS"); 
						 
					if (reltype.equalsIgnoreCase("CHANGE") && !relStatus.equalsIgnoreCase("WAPPR") && !relStatus.equalsIgnoreCase("CAN")){
						
						 relatedflag = true;
					} 
				}
			}
					
			if (relatedflag == false){  
					
				MboSetRemote CMPT = MXServer.getMXServer().getMboSet("CMPT", getUserInfo());
				CMPT.setWhere("SEVERITY=" +severity+ "and LIKEOFOCC ='" +lof+ "' and WOPRIORITY =" +priority);
				CMPT.reset(); 
					
				if (CMPT.count() > 0){
						
					tqdaydiff = CMPT.getMbo(0).getInt("TQDAYSDIFFERENCE");
					firstStatusDate = DateUtility.addDays(firstStatusDate, tqdaydiff);
					double newHrs = 0D;
					int min = 0;
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
					String defaultCal = MXServer.getMXServer().getConfig().getProperty("WorkingHoursCalendar");	
					MboSetRemote workPeriodSet = getMboSet("$CalSet", "WORKPERIOD", (new StringBuilder("CalNum = '")).append(defaultCal).append("' And WorkDate >= To_Date('").append(sdf.format(firstStatusDate.getTime())).append("', 'DD/MM/YYYY')").toString());
					workPeriodSet.setOrderBy("workdate");
					workPeriodSet.reset();
					WorkPeriodRemote workPeriodMbo = null;
					
					if(workPeriodSet.getMbo(0) != null){
						workPeriodMbo = (WorkPeriodRemote) workPeriodSet.getMbo(0);
						Calendar calEndTime = Calendar.getInstance();
						calEndTime.setTime(workPeriodMbo.getDate("ENDTIME"));  
						Calendar workDate = Calendar.getInstance();
						workDate.setTime(workPeriodMbo.getDate("WORKDATE"));
						calEndTime.set(workDate.get(1), workDate.get(2), workDate.get(5));
												
						this.setValue("TARGCOMPDATE", calEndTime.getTime(),11L);
						if((this.getString("STATUS").equalsIgnoreCase("WAPPR") || this.getString("STATUS").equalsIgnoreCase("WPREP") ) && this.getString("SCHEDFINISH") == "")
						{
								this.setValue("SCHEDFINISH", calEndTime.getTime(),11L);
						}
				    }
					
					if (this.getString("TARGCOMPDATE") != ""){
						MboSetRemote wkPeriodSet = getMboSet("$CalSet", "WORKPERIOD", (new StringBuilder("CalNum = '")).append(defaultCal).append("' And WorkDate <= To_Date('").append(sdf.format(this.getDate("TARGCOMPDATE"))).append("', 'DD/MM/YYYY')").toString());
						wkPeriodSet.setOrderBy("workdate desc");
						wkPeriodSet.reset();
						
						for (int k = 0; wkPeriodSet.getMbo(k) != null; k++){
							WorkPeriodRemote wkPeriodMbo = (WorkPeriodRemote) wkPeriodSet.getMbo(k);
							newHrs = newHrs + (double)wkPeriodMbo.getInt("WORKHOURS");
							
							Calendar st = Calendar.getInstance();
							st.setTime(wkPeriodMbo.getDate("STARTTIME")); 
							Calendar wDate = Calendar.getInstance();
							wDate.setTime(wkPeriodMbo.getDate("WORKDATE"));
							st.set(wDate.get(1), wDate.get(2), wDate.get(5));
							
							if(dur <= newHrs){
								if(dur == newHrs){
									this.setValue("TARGSTARTDATE", st.getTime(),11L);
									if((this.getString("STATUS").equalsIgnoreCase("WAPPR") || this.getString("STATUS").equalsIgnoreCase("WPREP")) && this.getString("SCHEDSTART") == "")
									{
										this.setValue("SCHEDSTART", st.getTime(),11L);
									}
									
									break;
								}
								else{
									newHrs = newHrs - dur;
									min = (int)(newHrs*60);
									st.add(Calendar.MINUTE, min);
									this.setValue("TARGSTARTDATE", st.getTime(),11L);
									if((this.getString("STATUS").equalsIgnoreCase("WAPPR") || this.getString("STATUS").equalsIgnoreCase("WPREP")) && this.getString("SCHEDSTART") == "")
									{
										this.setValue("SCHEDSTART", st.getTime(),11L);
									}
									break;
								}
							}
						}
					}
					if(this.getString("TQTARGSTARTDATE") == ""){
						this.setValue("TQTARGSTARTDATE", this.getDate("TARGSTARTDATE"),11L);
					}
					workPeriodSet.close();
				}
				CMPT.close();
			}				
		}
		woStatusSet.close();
	}

	public void populateTemp(MboRemote mbo) {
	     try{
	    	 if (mbo.toBeAdded() && (this.getString("JPNUM") == "" || mbo.getString("ROUTE") == "") 
					  && mbo.getString("WOCLASS").equalsIgnoreCase("WORKORDER") && 
							 ( mbo.getString("WORKTYPE") == "" ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("CM") ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("PJ") ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("BR") ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("TR") ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("PV") ||
									 mbo.getString("WORKTYPE").equalsIgnoreCase("DR-FM") ||
								     mbo.getString("WORKTYPE").equalsIgnoreCase("DR-HG") ||
								     mbo.getString("WORKTYPE").equalsIgnoreCase("DR-ST") ||
								     mbo.getString("WORKTYPE").equalsIgnoreCase("DR-RO") ||
								     mbo.getString("WORKTYPE").equalsIgnoreCase("DR-VA")))  
			 {		 
				   MboSetRemote TQworkTempSet = (MboSet)getMboServer().getMboSet("TQTEMLONDES", getUserInfo());
			 	   MboSetRemote TQworkTempSet1 = (MboSet)getMboServer().getMboSet("TQTEMLONDES", getUserInfo());
				   TQworkTempSet.setWhere("TQTEMPNAME='TAQA04'");
				   TQworkTempSet1.setWhere("TQTEMPNAME='TAQAWOJST'"); 
				   TQworkTempSet.reset();
				   TQworkTempSet1.reset();
					   if(TQworkTempSet.count()>0&&TQworkTempSet1.count()>0)
					    {
						   mbo.setValue("DESCRIPTION_LONGDESCRIPTION", TQworkTempSet.getMbo(0).getString("TQTEMLONGDES"), 11L);
						   mbo.setValue("JUSTIFYPRIORITY_LONGDESCRIPTION", TQworkTempSet1.getMbo(0).getString("TQTEMLONGDES"), 11L);
						}			        	
					    TQworkTempSet.close();
					    TQworkTempSet1.close();
			 }
				 
		}
		 catch(Exception e){
		 e.printStackTrace();
		 }
	}
	public void populateSDs(int priority, double dur) throws MXException, RemoteException {	
		
		String defaultCal1 = MXServer.getMXServer().getConfig().getProperty("WorkingHoursCalendar");
		SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy");
		 
		if (this.getString("SCHEDSTART") != ""){
			MboSetRemote wkPeriodSet1 = getMboSet("$CalSet", "WORKPERIOD", (new StringBuilder("CalNum = '")).append(defaultCal1).append("' And WorkDate >= To_Date('").append(sdf1.format(this.getDate("SCHEDSTART"))).append("', 'DD/MM/YYYY')").toString());
			wkPeriodSet1.setOrderBy("workdate asc");
			wkPeriodSet1.reset();
			Double nHrs = 0.0D;
			int min1 = 0;
			for (int j = 0; wkPeriodSet1.getMbo(j) != null; j++){
				WorkPeriodRemote wkPeriodMbo1 = (WorkPeriodRemote) wkPeriodSet1.getMbo(j);
				nHrs = nHrs + (double)wkPeriodMbo1.getInt("WORKHOURS");
				
				Calendar st1 = Calendar.getInstance();
				st1.setTime(wkPeriodMbo1.getDate("ENDTIME")); 
				Calendar wDate = Calendar.getInstance();
				wDate.setTime(wkPeriodMbo1.getDate("WORKDATE"));
				st1.set(wDate.get(1), wDate.get(2), wDate.get(5));
				
				if(dur <= nHrs){
					if(dur == nHrs){
						this.setValue("SCHEDFINISH", st1.getTime(),11L);
						this.getString("SCHEDFINISH");					
						break;
					}
					else{
						nHrs = dur-nHrs;
						min1 = (int)(nHrs*60);
						st1.add(Calendar.MINUTE, min1);
						st1.add(Calendar.SECOND, 1);
						if(this.getString("STATUS").equalsIgnoreCase("WAPPR") || this.getString("STATUS").equalsIgnoreCase("WPREP") || this.getString("SCHEDSTART") != "")
						{
							this.setValue("SCHEDFINISH", st1.getTime(),11L);
						}
						break;
					}
				}
			}
		}
	}

}