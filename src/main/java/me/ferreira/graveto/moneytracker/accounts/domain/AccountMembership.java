package me.ferreira.graveto.moneytracker.accounts.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.ferreira.graveto.common.jpa.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;

import java.util.UUID;

@Getter
@Setter
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "account_membership")
public class AccountMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_membership_id_seq")
    @SequenceGenerator(
            name = "account_membership_id_seq",
            sequenceName = "account_membership_id_seq",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "user_sid")
    private UUID userSid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MembershipRole role;

}
