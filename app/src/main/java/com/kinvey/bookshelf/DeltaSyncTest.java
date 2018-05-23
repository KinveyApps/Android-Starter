package com.kinvey.bookshelf;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class DeltaSyncTest extends GenericJson {

    public DeltaSyncTest(){};

    public DeltaSyncTest(long value){
        this.garmentBarcode = value;
    }

    public String getILocation() {
        return ILocation;
    }

    public void setILocation(String ILocation) {
        this.ILocation = ILocation;
    }

    @Key(Constants.ILOCATION)
    private String ILocation;

    public String getIRoute() {
        return IRoute;
    }

    public void setIRoute(String IRoute) {
        this.IRoute = IRoute;
    }

    @Key(Constants.IROUTE)
    private String IRoute;

    public long getGarmentBarcode() {
        return garmentBarcode;
    }

    public void setGarmentBarcode(long value) {
        this.garmentBarcode = value;
    }

    @Key(Constants.GARMENT_BARCODE)
    private long garmentBarcode;

    public long getPartner_SoldToID() {
        return Partner_SoldToID;
    }

    public void setPartner_SoldToID(long partner_SoldToID) {
        Partner_SoldToID = partner_SoldToID;
    }

    @Key(Constants.PARTNER_SOLDTO)
    private long Partner_SoldToID;

    public long getWearerSortNumber() {
        return wearerSortNumber;
    }

    public void setWearerSortNumber(long wearerSortNumber) {
        this.wearerSortNumber = wearerSortNumber;
    }

    @Key(Constants.WEARER_SORT_NUMBER)
    private long wearerSortNumber;

    public String getService_ContractGUID() {
        return Service_ContractGUID;
    }

    public void setService_ContractGUID(String service_ContractGUID) {
        Service_ContractGUID = service_ContractGUID;
    }

    @Key(Constants.SERVICE_CONTRACT_GUID)
    private String Service_ContractGUID;

    public String getService_ContractLineGUID() {
        return Service_ContractLineGUID;
    }

    public void setService_ContractLineGUID(String service_ContractLineGUID) {
        Service_ContractLineGUID = service_ContractLineGUID;
    }

    @Key(Constants.SERVICE_CONTRACTLINE_GUID)
    private String Service_ContractLineGUID;

    public long getServiceContractID() {
        return ServiceContractID;
    }

    public void setServiceContractID(long serviceContractID) {
        ServiceContractID = serviceContractID;
    }

    @Key(Constants.SERVICE__CONTRACT_ID)
    private long ServiceContractID;

    public String getWearerFirstName() {
        return wearerFirstName;
    }

    public void setWearerFirstName(String wearerFirstName) {
        this.wearerFirstName = wearerFirstName;
    }

    @Key(Constants.WEARER_FIRST_NAME)
    private String wearerFirstName;

    public String getWearerLastName() {
        return wearerLastName;
    }

    public void setWearerLastName(String wearerLastName) {
        this.wearerLastName = wearerLastName;
    }

    @Key(Constants.WEARER_LAST_NAME)
    private String wearerLastName;

    public String getGarmentStatusCode() {
        return garmentStatusCode;
    }

    public void setGarmentStatusCode(String garmentStatusCode) {
        this.garmentStatusCode = garmentStatusCode;
    }

    @Key(Constants.GARMENT_STATUS_CODE)
    private String garmentStatusCode;

    public String getGradeCode() {
        return gradeCode;
    }

    public void setGradeCode(String gradeCode) {
        this.gradeCode = gradeCode;
    }

    @Key(Constants.GRADE_CODE)
    private String gradeCode;

    public String getInServiceDate() {
        return inServiceDate;
    }

    public void setInServiceDate(String inServiceDate) {
        this.inServiceDate = inServiceDate;
    }

    @Key(Constants.INSERVICE_DATE)
    private String inServiceDate;

    public String getLastScannedDate() {
        return lastScannedDate;
    }

    public void setLastScannedDate(String lastScannedDate) {
        this.lastScannedDate = lastScannedDate;
    }

    @Key(Constants.LAST_SCANNED_DATE)
    private String lastScannedDate;

    public String getMaintenanceCode() {
        return maintenanceCode;
    }

    public void setMaintenanceCode(String maintenanceCode) {
        this.maintenanceCode = maintenanceCode;
    }

    @Key(Constants.MAINTENANCE_CODE)
    private String maintenanceCode;

    public String getSKUID() {
        return SKUID;
    }

    public void setSKUID(String SKUID) {
        this.SKUID = SKUID;
    }

    @Key(Constants.SKUID)
    private String SKUID;

    public long getWashCycles() {
        return washCycles;
    }

    public void setWashCycles(long washCycles) {
        this.washCycles = washCycles;
    }

    @Key(Constants.WASH_CYCLES)
    private long washCycles;

    public String getLastScannedStationCode() {
        return lastScannedStationCode;
    }

    public void setLastScannedStationCode(String lastScannedStationCode) {
        this.lastScannedStationCode = lastScannedStationCode;
    }

    @Key(Constants.LAST_SCANNED_STATION_CODE)
    private String lastScannedStationCode;

    public String getLoanerFlag() {
        return loanerFlag;
    }

    public void setLoanerFlag(String loanerFlag) {
        this.loanerFlag = loanerFlag;
    }

    @Key(Constants.LOANER_FLAG)
    private String loanerFlag;

}
