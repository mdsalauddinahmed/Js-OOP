// Encapsulation

class BankAccount {
    #balance = 0;

    deposit(amount){
        this.#balance += amount;
        return this.#balance;
    }

  getBalance(){

    return `$ ${this.#balance}`;
}

}


let account = new BankAccount();
console.log(account.getBalance())



//Abstraction
class CoffeeMachine{

    start(){

        retturn `Starting the machine...`
    }

    brewCoffee(){

        return ` brewing the coffee`
    }

    pressCoffee(){
        this.start();
        this.brewCoffee();
    }

}

let coffee = new CoffeeMachine();
console.log(coffee.brewCoffee())