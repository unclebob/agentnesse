{
  "type": "static",
  "tags": [],
  "help": null,
  "prune": false
}
#### To test This page three variables must be defined on the parent page: "numerator", "denominator" and "quotient"

#### Define variables on this sub page. They can be used on the main page
```agentnesse
define result
77.0
```
```agentnesse
define quotient4
${= ${numerator} / ${denominator} =}.0
```

print quotient  = ${quotient}
print quotient2 = ${quotient2}
print quotient4 = ${quotient4}

| fitnesse.slim.test.Division |
| --- | --- | --- | --- |
| # Test Description | numerator | denominator | quotient? |
| Expected result is calculated with expression | ${numerator} | ${denominator} | ${= %3.0f : ${numerator} / ${denominator} =}.0 |
| Expected result is in variable | ${numerator} | ${denominator} | ${quotient} |
| Store result in a symbol | ${numerator} | ${denominator} | $result= |
| Use symbol | $result | 1 | ${quotient} |

printing a variable is possible:   result = ${result}
printing a symbol is not possible: result = $result
