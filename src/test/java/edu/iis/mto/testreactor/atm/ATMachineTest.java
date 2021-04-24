package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationToken;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Currency;
import java.util.Locale;

@ExtendWith({MockitoExtension.class})
class ATMachineTest {
    
    @Mock
    private Bank bankMock;
    
    @Mock
    private MoneyDeposit moneyDepositMock;

    private static final int SAMPLE_DENOMINATION = 960;
    
    private static final Currency SAMPLE_PLN_CURRENCY =  Currency.getInstance("PLN");
    
    private static final Money SAMPLE_MONEY_IN_PLN = new Money(SAMPLE_DENOMINATION, SAMPLE_PLN_CURRENCY);
    
    private static final Currency SAMPLE_US_CURRENCY = Currency.getInstance(Locale.US);
    
    private static final Card SAMPLE_CARD = Card.create("1111222233334444");
    
    private static final PinCode SAMPLE_PIN_CODE = PinCode.createPIN(1, 2, 3, 4);
    
    private static final AuthorizationToken SAMPLE_AUTH_TOKEN = AuthorizationToken.create("SAMPLE AUTH TOKEN");
    
    @BeforeEach
    void setUp() throws Exception {}
    
    @Test
    public void shouldThrowATMOperationExceptionWhenAmountIsInvalid() {
        
        // given
        ATMachine atm = new ATMachine(bankMock,  SAMPLE_PLN_CURRENCY);
        when(moneyDepositMock.getCurrency()).thenReturn(SAMPLE_PLN_CURRENCY);
        atm.setDeposit(moneyDepositMock);
        
        Money moneyInUsCurrency = new Money(SAMPLE_DENOMINATION, SAMPLE_US_CURRENCY);
        
        // when
        try {
            atm.withdraw(SAMPLE_PIN_CODE, SAMPLE_CARD, moneyInUsCurrency);

            // then
            fail("Should throw ATMOperationException");
        } catch (ATMOperationException exception) { 
            assertEquals(exception.getErrorCode(), ErrorCode.WRONG_CURRENCY);
        }
    }
    
    @Test
    public void shouldInvokeBankAuthorizationWhenWithdrawing() throws AuthorizationException, ATMOperationException, AccountException {
        
        // given
        ATMachine atm = new ATMachine(bankMock, SAMPLE_PLN_CURRENCY);
        when(moneyDepositMock.getCurrency()).thenReturn(SAMPLE_PLN_CURRENCY);
        atm.setDeposit(moneyDepositMock);

        when(bankMock.autorize(SAMPLE_PIN_CODE.getPIN(), SAMPLE_CARD.getNumber())).thenReturn(SAMPLE_AUTH_TOKEN);
        doNothing().when(bankMock).charge(any(), any());
        
        // when
        atm.withdraw(SAMPLE_PIN_CODE, SAMPLE_CARD, Money.ZERO);

        // then
        verify(bankMock).autorize(SAMPLE_PIN_CODE.getPIN(), SAMPLE_CARD.getNumber());
    }
    
    @Test
    public void shouldThrowATMOperationExceptionWhenAuthorizationFailed() throws AuthorizationException {
        
        // given
        ATMachine atm = new ATMachine(bankMock, SAMPLE_PLN_CURRENCY);
        when(moneyDepositMock.getCurrency()).thenReturn(SAMPLE_PLN_CURRENCY);
        atm.setDeposit(moneyDepositMock);
        
        when(bankMock.autorize(SAMPLE_PIN_CODE.getPIN(), SAMPLE_CARD.getNumber())).thenThrow(new AuthorizationException());
        
        // when
        try {
            atm.withdraw(SAMPLE_PIN_CODE, SAMPLE_CARD, Money.ZERO);

            // then
            fail("Should throw ATMOperationException");
        } catch (ATMOperationException exception) {
            assertEquals(exception.getErrorCode(), ErrorCode.AHTHORIZATION);
        }
    }
    
    @Test
    public void shouldThrowExceptionWhenMoneyAmountIsNotWithdrawable() throws AuthorizationException {

        // given
        ATMachine atm = new ATMachine(bankMock, SAMPLE_PLN_CURRENCY);
        when(moneyDepositMock.getCurrency()).thenReturn(SAMPLE_PLN_CURRENCY);
        atm.setDeposit(moneyDepositMock);

        when(bankMock.autorize(SAMPLE_PIN_CODE.getPIN(), SAMPLE_CARD.getNumber())).thenReturn(SAMPLE_AUTH_TOKEN);
        
        when(moneyDepositMock.getAvailableCountOf(Banknote.PL_100)).thenReturn(2);
        when(moneyDepositMock.getAvailableCountOf(Banknote.PL_20)).thenReturn(2);
        
        Money invalidMoneyAmount = new Money(123, SAMPLE_PLN_CURRENCY);
        
        // when
        try {
            atm.withdraw(SAMPLE_PIN_CODE, SAMPLE_CARD, invalidMoneyAmount);
            
            // then
            fail("Should throw ATMOperationException");
        } catch (ATMOperationException exception) {
            assertEquals(exception.getErrorCode(), ErrorCode.WRONG_AMOUNT);
        }
    }
    
    @Test
    public void shouldReturnEmptyListWhenMoneyAmountIsZero() throws AuthorizationException, AccountException, ATMOperationException {
        // given
        ATMachine atm = new ATMachine(bankMock, SAMPLE_PLN_CURRENCY);
        when(moneyDepositMock.getCurrency()).thenReturn(SAMPLE_PLN_CURRENCY);
        atm.setDeposit(moneyDepositMock);

        when(bankMock.autorize(SAMPLE_PIN_CODE.getPIN(), SAMPLE_CARD.getNumber())).thenReturn(SAMPLE_AUTH_TOKEN);
        doNothing().when(bankMock).charge(any(), any());

        // when
        Withdrawal withdrawal = atm.withdraw(SAMPLE_PIN_CODE, SAMPLE_CARD, Money.ZERO);

        // then
        assertEquals(withdrawal.getBanknotes(), Collections.emptyList());
    }
//    
//    @Test
//    public void shouldWithdrawExpectedAmountOfMoney

}
