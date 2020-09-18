import random
import sqlite3

credit_card = 4000000000000000
user_pin = ''
nested_exit = False

conn = sqlite3.connect('card.s3db')
cur = conn.cursor()
cur.execute('CREATE TABLE IF NOT EXISTS card (id INTEGER, number TEXT, pin TEXT, balance INTEGER DEFAULT 0);')
conn.commit()

while True:
    if nested_exit:
        break
    print('1. Create an account')
    print('2. Log into account')
    print('0. Exit')
    choice = input()
    if choice == '1':
        user_id = random.randint(1, 999999999)
        temp_pin = random.randint(0, 9999)
        if temp_pin < 10:
            user_pin = '000' + str(temp_pin)
        elif temp_pin < 100:
            user_pin = '00' + str(temp_pin)
        elif temp_pin < 1000:
            user_pin = '0' + str(temp_pin)
        else:
            user_pin = str(temp_pin)
        credit_card += user_id
        number_list = list(str(credit_card))
        number_list.pop()
        luhn_sum = 0
        num_id = 1
        for i in number_list:
            if num_id % 2 != 0:
                luhn_num = int(i) * 2
            else:
                luhn_num = int(i)
            if luhn_num > 9:
                luhn_num -= 9
            luhn_sum += luhn_num
            num_id += 1
        if luhn_sum % 10 == 0:
            number_list.append('0')
        else:
            number_list.append(str(10 - (luhn_sum % 10)))
        credit_card = int(''.join(number_list))
        cur.execute('SELECT number FROM card')
        cards_count = len(cur.fetchall())
        cur.execute(f"SELECT number FROM card WHERE number = '{credit_card}';")
        same_card_check = cur.fetchone()
        if not same_card_check:
            cur.execute(f"INSERT INTO card VALUES ({cards_count+1}, '{str(credit_card)}', '{user_pin}', 0)")
            conn.commit()
            print('Your card has been created')
            print('Your card number:')
            print(credit_card)
            print('Your card PIN:')
            print(user_pin)
        else:
            credit_card = 4000000000000000
            user_pin = ''
            print('Error: Our program chose the existing credit card. Please, try again.')

    elif choice == '2':
        print('Enter your card number:')
        card_check = input()
        print('Enter your PIN:')
        pin_check = input()
        cur.execute(f"SELECT number, pin FROM card WHERE number = '{card_check}';")
        chosen_card_data = cur.fetchone()
        if chosen_card_data:
            if card_check == chosen_card_data[0] and pin_check == chosen_card_data[1]:
                print('You have successfully logged in!')
                while True:
                    print('1. Balance')
                    print('2. Add income')
                    print('3. Do transfer')
                    print('4. Close account')
                    print('5. Log out')
                    print('0. Exit')
                    log_choice = input()
                    if log_choice == '1':
                        cur.execute(f"SELECT balance FROM card WHERE number = '{card_check}';")
                        balance_check = cur.fetchone()
                        print(f'Balance: {balance_check[0]}')

                    elif log_choice == '2':
                        print('Enter income:')
                        try:
                            income = int(input())
                        except ValueError:
                            print('Error. Please, enter numbers only')
                            continue
                        cur.execute(f"SELECT balance FROM card WHERE number = '{card_check}';")
                        current_balance = cur.fetchone()
                        income += int(current_balance[0])
                        cur.execute(f"UPDATE card SET balance = {income} WHERE number = '{card_check}';")
                        conn.commit()
                        print('Income was added!')

                    elif log_choice == '3':
                        print('Transfer')
                        print('Enter card number:')
                        transfer_card = input()
                        luhn_sum = 0
                        num_id = 1
                        number_list = list(transfer_card)
                        cur.execute(f"SELECT number FROM card WHERE number = '{transfer_card}';")
                        transfer_card_check = cur.fetchone()
                        for i in number_list:
                            if num_id % 2 != 0:
                                luhn_num = int(i) * 2
                            else:
                                luhn_num = int(i)
                            if luhn_num > 9:
                                luhn_num -= 9
                            luhn_sum += luhn_num
                            num_id += 1
                        if card_check == transfer_card:
                            print("You can't transfer money to the same account!")
                            continue
                        elif luhn_sum % 10 != 0:
                            print('Probably you made a mistake in the card number. Please try again!')
                            continue
                        elif not transfer_card_check:
                            print('Such a card does not exist.')
                            continue
                        else:
                            print('Enter how much money you want to transfer:')
                            try:
                                transfer_amount = int(input())
                            except ValueError:
                                print('Error. Please, enter numbers only')
                                continue
                            cur.execute(f"SELECT balance FROM card WHERE number = '{card_check}';")
                            balance_check = cur.fetchone()
                            if balance_check[0] < transfer_amount:
                                print('Not enough money!')
                            else:
                                deducted_balance = balance_check[0] - transfer_amount
                                cur.execute(f"UPDATE card SET balance = {deducted_balance} WHERE number = '{card_check}';")
                                conn.commit()
                                cur.execute(f"SELECT balance FROM card WHERE number = '{transfer_card_check[0]}';")
                                transfer_card_balance = cur.fetchone()
                                increased_balance = transfer_card_balance[0] + transfer_amount
                                cur.execute(f"UPDATE card SET balance = {increased_balance} WHERE number = '{transfer_card_check[0]}';")
                                conn.commit()

                    elif log_choice == '4':
                        cur.execute(f"DELETE FROM card WHERE number = '{card_check}';")
                        conn.commit()
                        print('The account has been closed!')

                    elif log_choice == '5':
                        print('You have successfully logged out!')
                        break

                    elif log_choice == '0':
                        print('Bye!')
                        nested_exit = True
                        break
            else:
                print('Wrong card number or PIN!')
        else:
            print('Wrong card number or PIN!')

    elif choice == '0':
        print('Bye!')
        break

conn.commit()
conn.close()
