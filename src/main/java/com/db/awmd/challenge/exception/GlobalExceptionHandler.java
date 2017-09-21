package com.db.awmd.challenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler({InvalidAccountException.class, InsufficientBalanceException.class})
	public ResponseEntity<Object> handleMoneyTransferExceptions(RuntimeException ex){
		log.error(ex.getMessage());
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}
}