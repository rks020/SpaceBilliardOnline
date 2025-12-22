    private void drawAuroraNebula(Canvas canvas) {
        // Dynamic Aurora/Nebula effect for Space 7
        long time = System.currentTimeMillis();
        float wave1 = (float) Math.sin(time * 0.001) * 0.5f + 0.5f;
        float wave2 = (float) Math.cos(time * 0.0015) * 0.5f + 0.5f;
        
        // Deep space purple-blue gradient background
        paint.setStyle(Paint.Style.FILL);
        RadialGradient nebulaGradient = new RadialGradient(
            centerX, centerY * 0.5f, screenHeight,
            new int[] {
                Color.rgb(20, 10, 40),    // Deep purple
                Color.rgb(5, 10, 30),     // Dark blue
                Color.rgb(5, 5, 15)       // Almost black
            },
            new float[] { 0f, 0.6f, 1f },
            Shader.TileMode.CLAMP
        );
        paint.setShader(nebulaGradient);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setShader(null);
        
        // Aurora wave 1 (Green-Cyan)
        paint.setStyle(Paint.Style.FILL);
        int auroraColor1 = Color.argb(
            (int)(80 * wave1),
            0,
            (int)(200 + 55 * wave1),
            (int)(150 + 105 * wave1)
        );
        
        RadialGradient aurora1 = new RadialGradient(
            centerX, centerY + (wave1 * 200 - 100),
            screenWidth * 0.8f,
            auroraColor1,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        );
        paint.setShader(aurora1);
        canvas.drawCircle(centerX, centerY, screenWidth * 0.6f, paint);
        paint.setShader(null);
        
        // Aurora wave 2 (Purple-Magenta)
        int auroraColor2 = Color.argb(
            (int)(70 * wave2),
            (int)(150 + 105 * wave2),
            50,
            (int)(200 + 55 * wave2)
        );
        
        RadialGradient aurora2 = new RadialGradient(
            centerX * 0.7f, centerY - (wave2 * 150 - 75),
            screenWidth * 0.7f,
            auroraColor2,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        );
        paint.setShader(aurora2);
        canvas.drawCircle(centerX, centerY, screenWidth * 0.5f, paint);
        paint.setShader(null);
        
        // Aurora wave 3 (Blue-White shimmer)
        float wave3 = (float) Math.sin(time * 0.002 + Math.PI) * 0.5f + 0.5f;
        int auroraColor3 = Color.argb(
            (int)(60 * wave3),
            (int)(100 + 155 * wave3),
            (int)(150 + 105 * wave3),
            255
        );
        
        RadialGradient aurora3 = new RadialGradient(
            centerX * 1.3f, centerY + (wave3 * 180 - 90),
            screenWidth * 0.6f,
            auroraColor3,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        );
        paint.setShader(aurora3);
        canvas.drawCircle(centerX, centerY, screenWidth * 0.55f, paint);
        paint.setShader(null);
        
        // Nebula clouds (flowing particles effect)
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 8; i++) {
            float angle = (time * 0.00005f * (i + 1)) + (i * (float)Math.PI / 4);
            float distance = 300 + (i * 50);
            float cloudX = centerX + (float)Math.cos(angle) * distance;
            float cloudY = centerY + (float)Math.sin(angle) * distance;
            float cloudSize = 80 + (i * 15);
            
            int cloudAlpha = (int)(30 + 20 * Math.sin(time * 0.001 + i));
            int cloudColor = Color.argb(
                cloudAlpha,
                100 + (i * 15),
                50 + (i * 20),
                200 - (i * 10)
            );
            
            RadialGradient cloudGradient = new RadialGradient(
                cloudX, cloudY, cloudSize,
                cloudColor,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            );
            paint.setShader(cloudGradient);
            canvas.drawCircle(cloudX, cloudY, cloudSize, paint);
        }
        paint.setShader(null);
    }
