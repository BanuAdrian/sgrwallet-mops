package ro.mops.sgrwallet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vouchers",
       uniqueConstraints = @UniqueConstraint(name = "uk_vouchers_code", columnNames = "voucher_code"))
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @Column(name = "used_date")
    private Instant usedDate;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "added_date", nullable = false, updatable = false)
    private Instant addedDate;

    @Column(name = "voucher_code", nullable = false, length = 128)
    private String voucherCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_store_id", nullable = false)
    private BusinessStore businessStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherSource source = VoucherSource.MANUAL;

    public Voucher() {
    }

    @PrePersist
    void setAddedDate() {
        if (addedDate == null) {
            addedDate = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getUsedDate() {
        return usedDate;
    }

    public void setUsedDate(Instant usedDate) {
        this.usedDate = usedDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getAddedDate() {
        return addedDate;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BusinessStore getBusinessStore() {
        return businessStore;
    }

    public void setBusinessStore(BusinessStore businessStore) {
        this.businessStore = businessStore;
    }

    public VoucherSource getSource() {
        return source;
    }

    public void setSource(VoucherSource source) {
        this.source = source;
    }
}
