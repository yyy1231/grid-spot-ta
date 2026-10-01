package com.novax.entity.mysql;

import java.math.BigDecimal;
import java.util.Date;

public class GridSpotUserAssetsSnapshotEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long gridSpotOrderId;

    private String baseCurrency;

    private BigDecimal baseCurrencyAvailable;

    private BigDecimal baseCurrencyFrozen;

    private String quoteCurrency;

    private BigDecimal quoteCurrencyAvailable;

    private BigDecimal quoteCurrencyFrozen;

    private BigDecimal newPrice;

    private BigDecimal baseCurrencyToUsdtNewPrice;

    private BigDecimal quoteCurrencyToUsdtNewPrice;

    private Byte event;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGridSpotOrderId() {
        return gridSpotOrderId;
    }

    public void setGridSpotOrderId(Long gridSpotOrderId) {
        this.gridSpotOrderId = gridSpotOrderId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public BigDecimal getBaseCurrencyAvailable() {
        return baseCurrencyAvailable;
    }

    public void setBaseCurrencyAvailable(BigDecimal baseCurrencyAvailable) {
        this.baseCurrencyAvailable = baseCurrencyAvailable;
    }

    public BigDecimal getBaseCurrencyFrozen() {
        return baseCurrencyFrozen;
    }

    public void setBaseCurrencyFrozen(BigDecimal baseCurrencyFrozen) {
        this.baseCurrencyFrozen = baseCurrencyFrozen;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }

    public BigDecimal getQuoteCurrencyAvailable() {
        return quoteCurrencyAvailable;
    }

    public void setQuoteCurrencyAvailable(BigDecimal quoteCurrencyAvailable) {
        this.quoteCurrencyAvailable = quoteCurrencyAvailable;
    }

    public BigDecimal getQuoteCurrencyFrozen() {
        return quoteCurrencyFrozen;
    }

    public void setQuoteCurrencyFrozen(BigDecimal quoteCurrencyFrozen) {
        this.quoteCurrencyFrozen = quoteCurrencyFrozen;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public BigDecimal getBaseCurrencyToUsdtNewPrice() {
        return baseCurrencyToUsdtNewPrice;
    }

    public void setBaseCurrencyToUsdtNewPrice(BigDecimal baseCurrencyToUsdtNewPrice) {
        this.baseCurrencyToUsdtNewPrice = baseCurrencyToUsdtNewPrice;
    }

    public BigDecimal getQuoteCurrencyToUsdtNewPrice() {
        return quoteCurrencyToUsdtNewPrice;
    }

    public void setQuoteCurrencyToUsdtNewPrice(BigDecimal quoteCurrencyToUsdtNewPrice) {
        this.quoteCurrencyToUsdtNewPrice = quoteCurrencyToUsdtNewPrice;
    }

    public Byte getEvent() {
        return event;
    }

    public void setEvent(Byte event) {
        this.event = event;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}