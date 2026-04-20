FROM php:8.2-apache

RUN apt-get update && apt-get install -y --no-install-recommends \
    libzip-dev zip unzip git curl libpng-dev libonig-dev \
    libxml2-dev libcurl4-openssl-dev libssl-dev \
    && docker-php-ext-install pdo_mysql mysqli zip gd mbstring xml curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN a2enmod rewrite headers ssl

WORKDIR /var/www/html

# Configuração do Apache para permitir .htaccess
RUN echo '<Directory /var/www/html>\n\
    Options -Indexes +FollowSymLinks\n\
    AllowOverride All\n\
    Require all granted\n\
</Directory>' > /etc/apache2/conf-available/allow-override.conf \
    && a2enconf allow-override

RUN echo 'ServerName localhost' >> /etc/apache2/apache2.conf

# Configuração customizada do PHP
RUN { \
    echo 'upload_max_filesize = 50M'; \
    echo 'post_max_size = 50M'; \
    echo 'max_execution_time = 300'; \
    echo 'memory_limit = 256M'; \
    echo 'date.timezone = America/Sao_Paulo'; \
    echo 'display_errors = On'; \
    echo 'error_reporting = E_ALL'; \
    echo 'variables_order = "EGPCS"'; \
} > /usr/local/etc/php/conf.d/custom.ini

COPY . /var/www/html/

RUN mkdir -p /var/www/html/data/uploads /var/www/html/data/datasets /var/www/html/logs \
    && chown -R www-data:www-data /var/www/html \
    && chmod -R 755 /var/www/html \
    && chmod -R 775 /var/www/html/data

EXPOSE 80

CMD ["apache2-foreground"]
