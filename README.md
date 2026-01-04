# Jetbrains CLion CType Autocompletion Plugin

## Justification 
The plugin aims to suggest external symbols to the developer based on the CType object.

## Example
Given the mock library:
```c
#include <stdio.h>
int global_var;
int global_var_init = 26;

static int static_var;

int global_function(int p)
{
    static int local_static_var;
    static int local_static_var_init=5;

    local_static_var = p;

    return local_static_var_init + local_static_var;
}

int global_function2()
{
    int x;
    int y;
    return x+y;
}

#ifdef __cplusplus
extern "C"
#endif
void fun()
{
    static_var = 3;
    printf("hehe");
}
```

The IDE will suggest the exported function, i.e ```fun()```.

<img width="403" height="381" alt="AutocompletionExample" src="https://github.com/user-attachments/assets/229fe38d-471a-4060-af8e-f57b3dd1ed4b" />
