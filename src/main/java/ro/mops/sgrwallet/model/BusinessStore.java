package ro.mops.sgrwallet.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_stores",
       uniqueConstraints = @UniqueConstraint(name = "uk_business_stores_registry_number", columnNames = "registry_number"))
public class BusinessStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_store_id")
    private Long id;

    @Column(name = "business_store_name", nullable = false, length = 160)
    private String name;

    @Column(name = "registry_number", nullable = false, length = 32)
    private String registryNumber;

    @OneToMany(mappedBy = "managedStore")
    private List<User> managers = new ArrayList<>();

    @OneToMany(mappedBy = "businessStore")
    private List<Voucher> vouchers = new ArrayList<>();

    protected BusinessStore() {
    }

    public BusinessStore(String name, String registryNumber) {
        this.name = name;
        this.registryNumber = registryNumber;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistryNumber() {
        return registryNumber;
    }

    public void setRegistryNumber(String registryNumber) {
        this.registryNumber = registryNumber;
    }

    public List<User> getManagers() {
        return managers;
    }

    public List<Voucher> getVouchers() {
        return vouchers;
    }
}
