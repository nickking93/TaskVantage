# Stage 1: Build the Angular application
FROM node:20.9.0 AS build

WORKDIR /app

# Copy the package files and install dependencies
COPY package.json package-lock.json ./
RUN npm install

# Copy the entire project and build it
COPY . .

# Run the Angular build process
RUN npm run build -- --output-path=dist --configuration production

# Stage 2: Serve the Angular application using Nginx
FROM nginx:alpine

# Copy the nginx.conf file to override the default configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the built Angular application from the previous stage
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]
