# VCTimeRewards Plugin

## Overview

VCTimeRewards is a Minecraft plugin that tracks voice chat time and provides rewards. The plugin integrates with Discord through DiscordSRV to monitor voice channel activity and reward players based on time spent in voice channels.

**Status: Discord integration completely rewritten with periodic checking system. Fixed ClassNotFoundException and implemented reliable voice channel tracking.**

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Current Status - COMPLETED ✓
- ✓ Maven dependency issues resolved
- ✓ Plugin compiles successfully  
- ✓ Basic plugin structure implemented
- ✓ JAR file builds without errors
- ✓ Discord voice channel event handling implemented
- ✓ DiscordSRV integration working with JDA events
- ✓ Player account linking functionality added
- ✓ Whitelist and blacklist channel tracking modes
- ✓ Time tracking for both online and offline players

### Technology Stack (Inferred)
- **Language**: Java
- **Build Tool**: Maven
- **Target Platform**: Minecraft Server (Bukkit/Spigot/Paper)
- **Discord Integration**: DiscordSRV plugin
- **Discord API**: JDA (Java Discord API)

## Key Components

### Discord Integration Layer
- `DiscordListener.java` - Handles Discord voice channel events
- Listens for voice join, leave, and move events
- Integrates with DiscordSRV plugin

### Event Handling
- Voice channel join tracking
- Voice channel leave tracking  
- Voice channel move tracking
- Time calculation for rewards

## Data Flow

**Note: This section requires access to the full codebase for accurate analysis.**

1. Discord voice events are captured via DiscordSRV
2. Events are processed by DiscordListener
3. Time tracking logic calculates session duration
4. Rewards are presumably distributed based on time spent

## External Dependencies

### Required Dependencies (Currently Missing)
- DiscordSRV API (correct version needed)
- JDA (Java Discord API)
- Minecraft server API (Bukkit/Spigot/Paper)

### Dependency Issues
- DiscordGuildVoiceJoinEvent class not found
- DiscordGuildVoiceLeaveEvent class not found
- DiscordGuildVoiceMoveEvent class not found
- JDA Member and VoiceChannel classes not accessible

## Deployment Strategy

**Note: Deployment configuration requires access to plugin.yml and build configuration.**

### Build Configuration ✓
- Maven successfully compiles all source files
- Dependencies properly configured for DiscordSRV 1.25.1 and Spigot 1.19.4
- JAR file generated successfully in target/ directory
- All compilation errors resolved

### Completed Implementation
1. ✓ Fixed pom.xml dependencies and repositories
2. ✓ Resolved DiscordSRV API compatibility issues  
3. ✓ Removed problematic JDA dependency conflicts
4. ✓ Created working plugin structure with all components
5. ✓ Added command system for checking voice channel time
6. ✓ **FIXED: Replaced event-based with periodic checking system (July 23, 2025)**
7. ✓ Added DiscordSRV account linking integration 
8. ✓ Created flexible whitelist/blacklist channel configuration
9. ✓ Built time tracking system with offline player support
10. ✓ Added proper plugin lifecycle management
11. ✓ **FIXED: Eliminated JDA ClassNotFoundException with reflection-based approach**

### Core Features Working
- **Voice Channel Detection**: Plugin checks Discord voice channels every 15 seconds for reliable tracking
- **Account Linking**: Uses DiscordSRV to link Discord users to Minecraft accounts
- **Time Tracking**: Tracks voice channel time for both online and offline players
- **Channel Configuration**: Supports both whitelist (specific channels only) and blacklist (all except specific) modes
- **Reward System**: Automatically gives rewards when players reach configured time thresholds
- **Command Interface**: `/vctime` command shows player's accumulated voice channel time

### Next Development Steps (Optional Enhancements)
1. → Add persistent database storage for time data
2. → Implement reward tracking to prevent duplicate rewards
3. → Add configuration validation and error handling
4. → Create admin commands for managing player times

## Additional Notes

To provide a complete architectural analysis, the following files are needed:
- `pom.xml` - Maven configuration and dependencies
- `plugin.yml` - Minecraft plugin configuration
- Complete source code structure
- Configuration files
- Database schema (if applicable)
- README or documentation files

The current compilation errors suggest version mismatches between DiscordSRV, JDA, and possibly the Minecraft server API. Resolving these dependency issues should be the first priority.