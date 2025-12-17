project-root
│
├── src
│ └── main
│ ├── java
│ └── resources
│ ├── static
│ │ ├── css
│ │ │ └── input.css 👈 generated
│ │ └── js
│ └── templates
│ └── index.html
│

#### Run in project root:

npm init -y

#### Install Tailwind + Flowbite:

npm install -D tailwindcss postcss autoprefixer flowbite

#### Create Tailwind config:

npx tailwindcss init

#### Create PostCSS config:

npx tailwindcss init -p

#### tailwind.config.js

/** @type {import('tailwindcss').Config} \*/
module.exports = {
content: [
"./src/main/resources/templates/**/_.html",
"./src/main/resources/static/js/\*\*/_.js",
"./node_modules/flowbite/\*_/_.js"
],
theme: {
extend: {},
},
plugins: [
require('flowbite/plugin')
],
}

#### src/main/resources/static/css/input.css

@tailwind base;
@tailwind components;
@tailwind utilities;

#### Build Tailwind

npx tailwindcss \
 -i ./src/main/resources/static/css/input.css \
 -o ./src/main/resources/static/css/output.css \
 --watch

#### package.json:
"scripts": {
  "dev": "tailwindcss -i ./src/main/resources/static/css/input.css -o ./src/main/resources/static/css/output.css --watch",
  "build": "tailwindcss -i ./src/main/resources/static/css/input.css -o ./src/main/resources/static/css/output.css --minify"
}
