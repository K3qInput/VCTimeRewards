# VCTimeRewards Plugin

## Overview

VCTimeRewards is a Minecraft plugin that tracks voice chat time and provides rewards. The plugin integrates with Discord through DiscordSRV to monitor voice channel activity and reward players based on time spent in voice channels.

**Status: Maven compilation issues have been resolved. Basic plugin structure is complete and compiles successfully.**

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Current Status - RESOLVED ✓
- ✓ Maven dependency issues resolved
- ✓ Plugin compiles successfully  
- ✓ Basic plugin structure implemented
- ✓ JAR file builds without errors
- → Discord voice channel event handling needs implementation

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

### Next Development Steps
1. → Implement proper DiscordSRV voice channel event handling
2. → Add database storage for persistent time tracking
3. → Complete reward system integration
4. → Add configuration validation and error handling

## Additional Notes

To provide a complete architectural analysis, the following files are needed:
- `pom.xml` - Maven configuration and dependencies
- `plugin.yml` - Minecraft plugin configuration
- Complete source code structure
- Configuration files
- Database schema (if applicable)
- README or documentation files

The current compilation errors suggest version mismatches between DiscordSRV, JDA, and possibly the Minecraft server API. Resolving these dependency issues should be the first priority.