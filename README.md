# JSON-based Expression Language (JEL)

## What is JEL?

JEL is a self-expanding expression language designed to be
embedded within existing JSON and JSON-derivative languages.

It is designed to be used _inside of_ existing data formats,
rather than _on top of_ one existing format. This enables
JEL scripts to be queried and manipulated by any number of 
existing frameworks, including Jackson, GSON, and various 
other ecosystems.

## Why Use JEL?

JEL is primarily focused on providing simple inlining and 
tempating features to existing JSON configurations.

For example:

* Variable substitution and inlining
* Arithmetic expressions
* Data templates
* Compile-time schema validations (WIP)

JEL is capable of higher-level language concepts, including
loops, generators, IO, and eventually JVM bytecode generation,
but another language such as Jsonnet or JSLT might be better
for highly-complex configurations and scripting.

## Simple Inlining and Templating

JEL is ideal for eliminating redundancy in large configurations
with relational data.

For example, assume the following configuration in XJS:

```
animal: {
  type: cat
  color: orange
  name: Garfield
}
```

A JSON path expression may be used to query this data for
reuse in mulitple locations.

```
person: {
  name: John
  favorite pet: $animal.name
}
```

### Expanded Inline Syntax

Note that path components for keys are limited to word
characters (regex: `\w`). To reference fields with
special characters, either quote the key or use the
expanded inline syntax (`${}`).

```
values: {
  key with spaces: value
}

ref1: $values.'key with spaces'
ref2: ${values.key with spaces}
ref3: ${values.'key with spaces'}
```

## Arithmetic Expressions

The JEL processor will automatically evaluate any arithmetic
expressions embedded within a configuration.

For example,

```
world: {
  height: 128
}

structure: {
  height: $world.height - 10
}
```

The following operators are supported by the processor:

| Operator | Description                  | Example       |
| :------: | ---------------------------- | :-----------: |
| +        | Addition                     | 2 + 1         |
| -        | Subtraction                  | 3 - 2         |
| *        | Multiplication               | 4 * 3         |
| /        | Division                     | 5 / 4         |
| ^        | Power                        | 6 ^ 5         |
| %        | Modulus                      | 7 % 6         |
| \|       | Bitwise OR                   | 8 \| 7        |
| &        | Bitwise AND                  | 9 & 8         |
| >>       | Bitwise right shift          | 10 >> 9       |
| <<       | Bitwise left shift           | 11 << 10      |
| ()       | Parentheses (multiplication) | 12(11)        |
| ()       | Parentheses (order)          | 13 * (12 + 2) |

## Statemtents via Data Instructions

JEL supports a number of field flags which may be used as
instructions to perform various operations.

To set up a field for use as a statement, begin the key
with the `>>` or **Logical Alias** operator.

For example, to import values from another configuration:

```
>> import: config.xjs
```

If this configuration is an object, its keys will be copied
into the current file.

For example, assume the following configuration:

**config.xjs:**

```
world: {
  height: 128
}
```

**tutorial.xjs:**

```
>> import: config.xjs

generator: {
  height: $world.height
}
```

To import this configuration into an object, give it a name.

This _name_ is said to be the value's _logical alias_ because
its technical key is everything to the left of the `:`.

The logical alias may be used as the reference for this value.

```
config >> import: config.xjs

generator: {
  height: $config.world.height
}
```

## Data Templating

JEL can be used to define data templates. These may be used
like functions to repeatedly generate values.

For example, to define a template which generates an animal
object:

```
animal >> (type, color): {
  type: $type
  color: $color
}
```

To use this template, **call** it by passing any parameters
inside of parentheses (`()`).

```
animals: [
  $animal(cat, orange)
  $animal(dog, brown)
]
```

Note that any tokens passed inside of `,` will be inlined
directly as _xjs_ tokens, meaning raw tokens will be treated
as strings. To pass these values explicitly as strings, use
any other type of valid xjs string: (`""`, `''`, `""""""`).

```
animals: [
  $animal('bird', 'blue')
]
```

Data templates can be used to return any type of value.

For example, to write a function which adds 2 to any number:

```
add2 >> (number): $number + 2

four: $add2(2)
```

# JEl Flags

Jel provides a variety of field flags out of the box. These
flags can be used either as **instructions** or **modifiers**.

For example, to define a _variable_ (different from a regular
value), attach the `var` flag. This excludes the field from
the output.

```
first >> var: Bob
last >> var: Smith

name: $first $last
```

Exluding a field from the output does not prevent it from
being _exported_, meaning the value is still visible to
other configurations.

To prevent this, flag the field as `private`.

**utitlities.xjs:**

```
secret >> private: 3.14

circumference >> (radius): 
  2 * $radius * $secret
```

**tutorial.xjs:**

```
utils >> import: utilities.xjs

radius: 1.5
circumference: $utils.circumference($radius)
```

### Additional Flags

The following flags are also supported out of the box:

| Flag       | Description                                      |
| :--------: | ------------------------------------------------ |
| `var`      | Excludes the field from the output.              |
| `private`  | Hides the field from other files.                |
| `add`      | Adds values into another container.              |
| `set`      | Updates a value by its qualified path.           |
| `def`      | Defines a new value dynamically.                 |
| `import`   | Reads data from another file.                    |
| `export`   | Exports data into another file. (TBD)            |
| `noinline` | Skips evaluation for one field.                  |
| `meta`     | Attaches a config to another expression.         |
| `from`     | Destructures a container into multiple values.   |
| `log`      | Prints data to the standard output.              |
| `jel`      | Configures the JEL processor for one file. (TBD) |

### Combining Multiple Flags

JEL places no restrictions on the nubmer of flags. Expressions 
will be evaluated in the order in which they are written, but
any flags generally do not benefit from order.

For example, to import values from another file and prevent
them from being re-exported:

```
>> import var: config.xjs
```

## Conditional Expressions and Statements

JEL supports conditional expressions and statements via the
`if` operator. 

For example, to conditionally execute a statement with side
effects:

```
config >> import var: config.xjs

>> if ($config.height > 100): {
  >> log: Using an extremely tall structure!
}
```

Or, to use a compressed syntax by combining flags:

```
config >> import var: config.xjs

>> if ($config.height > 100) log: 
  Using an extremely tall structure!
```

For a more advanced conditional tree, place the `if` token
at the end of the key. The value of this expression is an
object where each key is the condition.

```
height: 150

size >> if: {
  $height < 50: small
  $height < 100: large
  _: giant
}
```

Finally, to compare values by equality, use a `match` expression:

```
fruit: banana

preference >> match $fruit: {
  apple: My favorite
  banana: Second favorite
  orange: Third favorite
  _: Not interested
}
```

`match` expressions can be combined with single conditions as guards:

```
fruit: banana

preference >> match $fruit: {
  apple >> if ($rand() < 0.50):
    Apple and lucky!
  _:
    Not an apple or not lucky.
}
```

`match` statements can be used to check objects for specific fields
and arrays for any number of elements.

```
fruit: {
  type: banana
  color: yellow
}

preference >> match $fruit: {
  {type: banana}: I like it!
  _: I don't like it or type mismatch
}
```

Note that presently, conditional expressions
are only possible in objects. There is currently no syntax
for conditional expressions inside of arrays.

## Generators and Loops

JEL supports a handful of advanced scripting concepts
including loops and generators.

### Array Generators

Array generators (and regular loops) are written by placing
an array after the logical alias operator.

For example, to loop over a set of pre-defined values:

```
>> [1, 2, 3]: {
  >> log: [
    Current value: $v
    Current index: $i
  ]
}
```

Or, combine operators to get the highest number:

```
max: null

>> [1, 2, 3] if ($v > $max): {
  max >> set: $v
}
```

To generate an array, simply provide an alias:

```
squares >> [1, 2, 3]: $v ^ 2
```

Copy values from another array:

```
input: [1, 2, 3]
cubes >> [$input..]: $v ^ 3
```

Combine operators to filter values:

```
odds >> [1, 2, 3] if ($v %2 != 0): $v
``` 

### Object Generators

To iterate over the keys, values, and indices of an object:

```
>> {type: baloon, color: red}: {
  >> log: [
    key: $k
    value: $v
    index: $i
  ]
}
```

To generate an object, provide an alias:

```
numbers >> {one: 1, two: 2, three: 3}: {
  number_$k >> def: $v
}
```

Copy values from another object:

```
input: {one: 1, two: 2, three: 3}

numbers: {$input..}: {
  number_$k >> def: $v
}
```

Combine operators to filter keys and values:

```
odds >> {one: 1, two: 2, three: 3} 
        if ($v % 2 != 0): {
  number_$k >> def: $v
}
```

Note that in xjs, a key does not end until the first
exposed `:`, so these definitions may run onto more
than one line:

```
numbers >> {
  one: 1
  two: 2 
  three: 3
}: {
  number_$k >> def: $v
}
```

## Destructuring

In JEL, objects and arrays may be destructured in
order to extract values by pattern.

For example, to copy values from another object:

```
config: {
  world: { height: 128 }
  structure: { type: nbt }
  metadata: { author: PersonTheCat }
}

{world, structure} >> from: $config
```

Combine operators to get a handful of specific keys 
from an `import` expression:

```
{world, structure} >> import from: config.xjs
```

Use a very explicit syntax to declare these values
as variables: 

```
{world} >> import var from: config.xjs
```

To copy values from an array:

```
[first, second, _, fourth] >> from: [ 1, 2, 3, 4 ]
```

## Functions

While JEL does provide a syntax for declaring
_templates_, these are not true functions.

Extension functions are supported by the ecosystem,
but these must be defined in-code.

### Provided Static Functions

JEL provides a handful of static methods for
interacting with, querying, and generating data.

The language specification does provide support
for overloaded functions, so several of these
functions may have the same identifier.


<table>

  <tr>
    <th>Signature</th>
    <th>Description</th>
  </tr>
  
  <tr>
    <td><code>max()</code></td>
    <td>Returns the highest possible value.</td>
  </tr>
  
  <tr>
    <td><code>max(a: number, b: number)</code></td>
    <td>Returns the highest of 2 values.</td>
  </tr>
  
  <tr>
    <td><code>max(a: array)</code></td>
    <td>Returns the highest value from an array</td>
  </tr>
  
  <tr>
    <td><code>min()</code></td>
    <td>Returns the lowest possible value.</td>
  </tr>
  
  <tr>
    <td><code>min(a: number, b: number)</code></td>
    <td>Returns the lowest of 2 values./td>
  </tr>
  
  <tr>
    <td><code>min(a: array)</code></td>
    <td>Returns the lowest value from an array.</td>
  </tr>
  
  <tr>
    <td><code>rand()</code></td>
    <td>Returns a random decimal from 0 to 1.</td>
  </tr>
  
  <tr>
    <td><code>rand(minIn: number, maxEx: number)</code></td>
    <td>
      Returns a random integer with inclusive lower
      and exclusive higher bounds.
    </td>
  </tr>
  
  <tr>
    <td><code>rand(a: array)</code></td>
    <td>Returns a random element from an array.</td>
  </tr>
  
</table> 

### Provided Instance Functions

JEL also provides a handful of instance functions. 

<table>

  <tr>
    <th>Signature</th>
    <th>Description</th>
  </tr>
  
  <tr>
    <td><code>hash()</code></td>
    <td>Generates an integer hash from the value.</td>
  </tr>
  
  <tr>
    <td><code>startsWith(value: any)</code></td>
    <td>
      Returns true if a number, string, array, or object
      <em>starts with</em> the other.
    </td>
  </tr>
  
  <tr>
    <td><code>endsWith(value: any)</code></td>
    <td>
      Returns true if a number, string, array, or object
      <em>starts with</em> the other.
    </td>
  </tr>
  
  <tr>
    <td><code>contains(value: any)</code></td>
    <td>
      Returns true if a number, string, array, or object
      <em>contains</em> the other.
    </td>
  </tr>
  
  <tr>
    <td><code>size()</code></td>
    <td>Returns the number of elements or length of a value.</td>
  </tr>

  <tr>
    <td><code>coerce(values: any...)</code></td>
    <td>Returns the first nonnull parameter.</td>
  </tr>
  
  <tr>
    <td><code>matches(regex: string)</code></td>
    <td>
      Returns true if the string form of this value
      <em>matches</em> the given regular expression.
    </td>
  </tr>
  
  <tr>
    <td><code>replace(regex: string, value: string)</code></td>
    <td>Replaces all matches with the given replacement.</td>
  </tr>
  
  <tr>
    <td><code>uppercase()</code></td>
    <td>Coerces this value into an all-caps string.</td>
  </tr>

  <tr>
    <td><code>lowercase()</code></td>
    <td>Coerces this value into a lowercase string.</td>
  </tr>

  <tr>
    <td><code>trim()</code></td>
    <td>
      Coerces this value into a string and removes any
      leading or trailing whitespace.
    </td>
  </tr>

  <tr>
    <td><code>isNumber()</code></td>
    <td>Returns true if the value is a number.</td>
  </tr>
  
  <tr>
    <td><code>isBoolean()</code></td>
    <td>Returns true if the value is a boolean.</td>
  </tr>
  
  <tr>
    <td><code>isString()</code></td>
    <td>Returns true if the value is a String.</td>
  </tr>
  
  <tr>
    <td><code>isObject()</code></td>
    <td>Returns true if the value is an object.</td>
  </tr>
  
  <tr>
    <td><code>isArray()</code></td>
    <td>Returns true if the value is an array.</td>
  </tr>
  
  <tr>
    <td><code>isNull()</code></td>
    <td>Returns true if the value is null.</td>
  </tr>
  
  <tr>
    <td><code>intoNumber()</code></td>
    <td>Coerces the value into a number.</td>
  </tr>
  
  <tr>
    <td><code>intoBoolean()</code></td>
    <td>Coerces the value into a boolean.</td>
  </tr>
  
  <tr>
    <td><code>intoString()</code></td>
    <td>Coerces the value into a String.</td>
  </tr>
  
  <tr>
    <td><code>intoObject()</code></td>
    <td>Coerces the value into an object.</td>
  </tr>
  
  <tr>
    <td><code>intoArray()</code></td>
    <td>Coerces the value into an array.</td>
  </tr>

</table>

**Instance functions must be called from an identifier.**

```
// Illegal syntax. This will fail to compile.
// illegal: [1, 2, 3].hash()

array: [ 1, 2, 3 ]
legal: $array.hash()
```

## Meta Expressions

In JEL, some expressions have a meta-form which takes a
configuration on the right-hand side. To use these variants,
provide the `meta` tag anywhere on the left-hand side.

For example, to use a meta template:

```
type >> meta (input): {
  
  // Any condition not met raises an error.
  validations: {
     $input.isNull(): I don't like null values!
  }
  
  // Perform any side effects in order.
  >> log: Input hash: $input.hash()
  
  // The final value of this field is 
  // returned by the template.
  return >> if: {
    $input.isArray() || $input.isObject():
      container
    _:
      not a container
  }
}
```

Meta generators have the same fields.

```
array >> meta [1, 2, 3]: {

  // Perform side effects and validations.
  >> if ($v % 2 == 0) log: Even number: $v
  
  // The final value of this field is
  // the next element.
  return: $v * 2
}
```

Meta imports allow parsing unrecognized extensions:

```
>> meta import: {
  file: unknown.ext
  type: xjs
}
```









