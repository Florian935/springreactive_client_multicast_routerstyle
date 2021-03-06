package com.florian935.client.share.router.service;

import com.florian935.client.share.router.domain.Supplier;
import com.florian935.client.share.router.domain.SupplierTransaction;
import com.florian935.client.share.router.domain.SupplierTransactionEvent;
import com.florian935.client.share.router.domain.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class SupplierTransactionServiceImpl implements SupplierTransactionService {

    private static final long INTERVAL_IN_MILLIS = 1000;
    private final TransactionService transactionService;
    private final Flux<Supplier> intervalSupplierTicker;

    public SupplierTransactionServiceImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
        this.intervalSupplierTicker = generateSupplierIntervalOf(INTERVAL_IN_MILLIS);
    }

    @Override
    public Flux<SupplierTransaction> transactionSupplierStream() {
        return transactionService
                .transactionStream()
                .map(this::transactionSupplierBuilder);
    }

    private SupplierTransaction transactionSupplierBuilder(
            final TransactionEvent transactionEvent) {
        return SupplierTransaction.builder()
                .supplier(randomSupplier())
                .transactionEvent(transactionEvent)
                .build();
    }

    private Supplier randomSupplier() {
        return Supplier.builder()
                .id(UUID.randomUUID().toString())
                .userName(randomUsername())
                .build();
    }

    private String randomUsername() {
        final String[] userNameList = "Lindsay,Flo,Ludo,Nico,Samy".split(",");
        return userNameList[new Random().nextInt(userNameList.length)];
    }

    @Override
    public Flux<SupplierTransactionEvent> transactionSupplierEventStream() {
        return Flux
                .zip(transactionService.transactionStream(), intervalSupplierTicker)
                .map(this::generateTransactionSupplierEventFlux);
    }

    private Flux<Supplier> generateSupplierIntervalOf(Long intervalInMillis) {
        return Flux
                .interval(Duration.ofMillis(intervalInMillis))
                .flatMap(ticker -> Flux.just(generateRandomSupplier()))
                .share();
    }

    private Supplier generateRandomSupplier() {
        return Supplier.builder()
                .id(UUID.randomUUID().toString())
                .userName(randomUsername())
                .build();
    }

    private SupplierTransactionEvent generateTransactionSupplierEventFlux(
            final Tuple2<TransactionEvent, Supplier> tupleOfZip) {
        return SupplierTransactionEvent.builder()
                .supplierCounter(tupleOfZip.getT1().getTickerNumber())
                .username(tupleOfZip.getT2().getUserName())
                .price(formatFloatNumberWithCustomDecimal(
                        tupleOfZip.getT1().getTransaction().getPrice(),
                        4))
                .instant(Instant.now())
                .build();
    }

    private float formatFloatNumberWithCustomDecimal(final float numberToFormat, final int numberOfDecimal) {
        final NumberFormat numberFormat = getNumberFormatWithMaxFractionDigitsAndLocal(
                Locale.ENGLISH,
                numberOfDecimal);

        return Float.parseFloat(numberFormat.format(numberToFormat));
    }

    private NumberFormat getNumberFormatWithMaxFractionDigitsAndLocal(final Locale locale, final int numberOfDecimal) {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(numberOfDecimal);
        return numberFormat;
    }
}
