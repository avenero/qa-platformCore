#!/bin/bash

# Script para limpiar y optimizar el proyecto para IntelliJ IDEA
# Ejecutar: ./clean-ide.sh

echo "🧹 Limpiando proyecto para optimizar IntelliJ IDEA..."

# Limpiar builds de Gradle
echo "📦 Limpiando builds de Gradle..."
./gradlew clean

# Limpiar directorios temporales
echo "🗑️  Eliminando archivos temporales..."
find . -type d -name "build" -prune -exec rm -rf {} \; 2>/dev/null
find . -type d -name ".gradle" -prune -exec rm -rf {} \; 2>/dev/null
find . -type d -name "out" -prune -exec rm -rf {} \; 2>/dev/null

# Limpiar archivos .DS_Store de macOS
echo "🍎 Limpiando archivos .DS_Store..."
find . -name ".DS_Store" -type f -delete

# Limpiar logs antiguos (opcional)
echo "📝 Limpiando logs antiguos..."
find . -name "*.log" -type f -mtime +7 -delete 2>/dev/null

echo ""
echo "✅ Limpieza completada!"
echo ""
echo "📌 Próximos pasos en IntelliJ IDEA:"
echo "   1. File > Invalidate Caches > Invalidate and Restart"
echo "   2. File > Project Structure > Modules"
echo "   3. Marca como 'Excluded' (rojo): build/, .gradle/, .idea/"
echo "   4. Help > Edit Custom VM Options (aumentar memoria a 4GB)"
echo ""
echo "🚀 Después de estos pasos, el IDE debería funcionar más rápido."

