package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @InjectMocks
  @Autowired
  private AccountsService accountsService;

  @Mock
  private NotificationService notificationService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(accountsService);
  }

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void transferAmount() throws Exception {
    Account source = new Account("Id-1");
    source.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(source);

    Account destination = new Account("Id-2");
    destination.setBalance(new BigDecimal(500));
    this.accountsService.createAccount(destination);

    MoneyTransfer moneyTransfer = new MoneyTransfer("Id-1", "Id-2", new BigDecimal(400));
    this.accountsService.transferAmount(moneyTransfer);

    assertThat(this.accountsService.getAccount("Id-1").getBalance()).isEqualTo(new BigDecimal(600));
    assertThat(this.accountsService.getAccount("Id-2").getBalance()).isEqualTo(new BigDecimal(900));
    Mockito.verify(notificationService, Mockito.times(2)).notifyAboutTransfer(any(Account.class), anyString());
  }

  @Test(expected = InvalidAccountException.class)
  public void transferAmountToInvalidAccount() throws Exception {
    Account destination = new Account("Id-3");
    destination.setBalance(new BigDecimal(500));
    this.accountsService.createAccount(destination);

    MoneyTransfer moneyTransfer = new MoneyTransfer("Id-4", "Id-3", new BigDecimal(400));
    this.accountsService.transferAmount(moneyTransfer);
  }

  @Test(expected = InsufficientBalanceException.class)
  public void transferAmountFromInsufficientBalanceAccount() throws Exception {
    Account source = new Account("Id-4");
    source.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(source);

    Account destination = new Account("Id-5");
    destination.setBalance(new BigDecimal(500));
    this.accountsService.createAccount(destination);

    MoneyTransfer moneyTransfer = new MoneyTransfer("Id-4", "Id-5", new BigDecimal(400));
    this.accountsService.transferAmount(moneyTransfer);
  }

}
