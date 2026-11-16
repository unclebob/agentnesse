This section will contain custom resources (css, templates).

NOTE: You can not upload files to files/agnesse via AgNesse. Instead add
files directly.

You can override files in files/agnesse or add your own (e.g. a custom
theme). Both the render engine (Velocity) and the FileResponder check
files/agnesse and the agnesse.resources package when looking for a particular
file.

Say you want to add a custom theme called "custom". What do you do?

 1. Create AgNesseRoot/files/agnesse/css/custom.css for styling
 2. Create AgNesseRoot/files/agnesse/javascript/custom.js for behaviour
    (optional)
 3. Set the theme in agnesse.json

        "theme": "custom"
        
 4. Edit the style (e.g. to match the corporate standard)

If you're using Maven for example, you might want to distribute the style to
AgNesse projects. In that case you can create a jar with the styles in it:

        /agnesse/resources/css/custom.css
        /agnesse/resources/javascript/custom.js

and provide it as a dependency when launching AgNesse. You still have to
set "theme" in agnesse.json.
