## Requirements
Project was created with:
- Java version: 21.0.1
- Apache Maven 3.9.2
## Build:
```maven
mvn clean package
```
## Tests
Test can only be run by this maven command.
```maven
mvn test
```
## Mathematical description
Firstly with model 1 I find optimal number of delivery types for given items.
Then for all delivery types, that given items can be shipped I run model 2.
From model 2 solutions I pick solution with the greatest objective value and that's optimal solution.
### Model 1:
#### Constants
- **n** - number of items, limits variable **i** *(0 <= i < n)*
- **m** - number of delivery types, limits variable **j** *(0 <= j < m)*
#### Variables
- **item<sub>i,j</sub>**: Binary variable that represent whether item **i** is delivered by delivery method **j**.
- **delivery<sub>j</sub>**: Binary variable that represent whether delivery **j** is used in solution.
#### Objective
Minimize sum by **j** of **delivery<sub>j</sub>**
#### Constraints
- Sum by **j** of **item<sub>i,j</sub>** = 1 for all **i** 
- Sum by **i** of **item<sub>i,j</sub>** - (n+1)*delivery<sub>j</sub> <= 0 (this makes delivery<sub>j</sub> 1 if at least one item was used with delivery type **j**)
- **item<sub>i,j</sub>** = 0 if item **i** cannot be delivered by delivery method **j**

### Model 2:
#### Constants
- **deliveryNumber** - number of delivery types in optimal solution
- **deliveryIndex** - index of maximized delivery type
- **n** - number of items, limits variable **i** *(0 <= i < n)*
- **m** - number of delivery types, limits variable **j** *(0 <= j < m)*
#### Variables
- **item<sub>i,j</sub>**: Binary variable that represent whether item **i** is delivered by delivery method **j**.
- **delivery<sub>j</sub>**: Binary variable that represent whether delivery **j** is used in solution.
#### Objective
Maximize sum by **i** of item<sub>i,deliveryIndex</sub>
#### Constraints
- Sum by **j** of **item<sub>i,j</sub>** = 1 for all **i**
- Sum by **i** of **item<sub>i,j</sub>** - (n+1)*delivery<sub>j</sub> <= 0 (this makes delivery<sub>j</sub> 1 if at least one item was used with delivery type **j**)
- **item<sub>i,j</sub>** = 0 if item **i** cannot be delivered by delivery method **j**
- Sum by **j** of **delivery<sub>j<sub>** = **deliveryNumber**
