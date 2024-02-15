package main

import (
	"database/sql"
	"fmt"
	"log"
	"math/rand"
	"strconv"

	_ "github.com/mattn/go-sqlite3"
)

const (
	creditCardPrefix = 4000000000000000
	pinBound         = 10000
)

func main() {
	db, err := sql.Open("sqlite3", "card.s3db")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	createTable(db)

	random := rand.New(rand.NewSource(42))

	for {
		fmt.Println("1. Create an account")
		fmt.Println("2. Log into account")
		fmt.Println("0. Exit")

		var choice string
		fmt.Scanln(&choice)

		switch choice {
		case "1":
			cardNumber := generateCardNumber(random)
			pin := random.Intn(pinBound)
			paddedPin := fmt.Sprintf("%04d", pin)
			if createAccount(db, cardNumber) {
				fmt.Println("Your card has been created")
				fmt.Println("Your card number:")
				fmt.Println(cardNumber)
				fmt.Println("Your card PIN:")
				fmt.Println(paddedPin)
			} else {
				fmt.Println("Error: Our program chose the existing credit card. Please, try again.")
			}
		case "2":
			fmt.Println("Enter your card number:")
			var cardNumber int64
			fmt.Scanln(&cardNumber)
			fmt.Println("Enter your PIN:")
			var pin string
			fmt.Scanln(&pin)
			if logIn(db, cardNumber, pin) {
				if performTransactions(db, cardNumber) {
					return
				}
			} else {
				fmt.Println("Wrong card number or PIN!")
			}
		case "0":
			fmt.Println("Bye!")
			return
		}
	}
}

func createTable(db *sql.DB) {
	_, err := db.Exec("CREATE TABLE IF NOT EXISTS card (" +
		"id INTEGER, " +
		"number TEXT, " +
		"pin TEXT, " +
		"balance INTEGER DEFAULT 0);")
	if err != nil {
		log.Fatal(err)
	}
}

func generateCardNumber(random *rand.Rand) int64 {
	cardNumber := creditCardPrefix + int64(random.Intn(900000000)+100000000)
	digits := strconv.FormatInt(cardNumber, 10)
	var sum int
	for i, c := range digits {
		digit, _ := strconv.Atoi(string(c))
		if (len(digits)-i)%2 == 0 {
			digit *= 2
			if digit > 9 {
				digit -= 9
			}
		}
		sum += digit
	}
	checksum := 0
	if sum%10 != 0 {
		checksum = 10 - sum%10
	}
	return cardNumber*10 + int64(checksum)
}

func createAccount(db *sql.DB, cardNumber int64) bool {
	var number int64
	err := db.QueryRow("SELECT number FROM card WHERE number = ?", cardNumber).Scan(&number)
	if err != nil {
		if err == sql.ErrNoRows {
			return true
		}
		log.Fatal(err)
	}
	return false
}

func logIn(db *sql.DB, cardNumber int64, pin string) bool {
	var number int64
	var storedPin string
	err := db.QueryRow("SELECT number, pin FROM card WHERE number = ?", cardNumber).Scan(&number, &storedPin)
	if err != nil {
		if err == sql.ErrNoRows {
			return false
		}
		log.Fatal(err)
	}
	return storedPin == pin
}

func performTransactions(db *sql.DB, cardNumber int64) bool {
	for {
		fmt.Println("1. Balance")
		fmt.Println("2. Add income")
		fmt.Println("3. Do transfer")
		fmt.Println("4. Close account")
		fmt.Println("5. Log out")
		fmt.Println("0. Exit")

		var choice string
		fmt.Scanln(&choice)
		switch choice {
		case "1":
			displayBalance(db, cardNumber)
		case "2":
			addIncome(db, cardNumber)
		case "3":
			transferMoney(db, cardNumber)
		case "4":
			closeAccount(db, cardNumber)
			return false
		case "5":
			fmt.Println("You have successfully logged out!")
			return true
		case "0":
			fmt.Println("Bye!")
			return true
		}
	}
}

func displayBalance(db *sql.DB, cardNumber int64) {
	var balance int
	err := db.QueryRow("SELECT balance FROM card WHERE number = ?", cardNumber).Scan(&balance)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Balance:", balance)
}

func addIncome(db *sql.DB, cardNumber int64) {
	fmt.Println("Enter income:")
	var income int
	fmt.Scanln(&income)
	_, err := db.Exec("UPDATE card SET balance = balance + ? WHERE number = ?", income, cardNumber)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Income was added!")
}

func transferMoney(db *sql.DB, cardNumber int64) {
	fmt.Println("Transfer")
	fmt.Println("Enter card number:")
	var targetCard int64
	fmt.Scanln(&targetCard)
	if targetCard == cardNumber {
		fmt.Println("You can't transfer money to the same account!")
		return
	}
	if !validateCard(targetCard) {
		fmt.Println("Probably you made a mistake in the card number. Please try again!")
		return
	}
	fmt.Println("Enter how much money you want to transfer:")
	var amount int
	fmt.Scanln(&amount)
	if hasEnoughBalance(db, cardNumber, amount) {
		_, err := db.Exec("UPDATE card SET balance = balance - ? WHERE number = ?", amount, cardNumber)
		if err != nil {
			log.Fatal(err)
		}
		_, err = db.Exec("UPDATE card SET balance = balance + ? WHERE number = ?", amount, targetCard)
		if err != nil {
			log.Fatal(err)
		}
		fmt.Println("Transfer completed!")
	} else {
		fmt.Println("Not enough money!")
	}
}

func validateCard(cardNumber int64) bool {
	cardStr := strconv.FormatInt(cardNumber, 10)
	var sum int
	for i, c := range cardStr {
		digit, _ := strconv.Atoi(string(c))
		if (len(cardStr)-i)%2 == 0 {
			digit *= 2
			if digit > 9 {
				digit -= 9
			}
		}
		sum += digit
	}
	return sum%10 == 0
}

func hasEnoughBalance(db *sql.DB, cardNumber int64, amount int) bool {
	var balance int
	err := db.QueryRow("SELECT balance FROM card WHERE number = ?", cardNumber).Scan(&balance)
	if err != nil {
		log.Fatal(err)
	}
	return balance >= amount
}

func closeAccount(db *sql.DB, cardNumber int64) {
	_, err := db.Exec("DELETE FROM card WHERE number = ?", cardNumber)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("The account has been closed!")
}
