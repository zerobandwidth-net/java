package net.zerobandwidth.lang ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a framework for runnable classes that can also be executed as
 * console applications.
 * 
 * <p>While this class does not provide a concrete implementation of the usual
 * {@code main(String[])} method, it is designed so that such a method would
 * simply instantiate the implementation class and then execute its
 * {@link #run} method.</p>
 * 
 * <pre>
 * public static void main( String[] asArgs )
 * {
 *     MyAppClass app = new MyAppClass() ;
 *     app.processArgs(asArgs).run() ;
 * }
 * </pre>
 * 
 * <h3>Command Line Input Model</h3>
 * 
 * <p>The model for processing command line arguments separates them into three
 * usage classes: switches, parameters, and values.</p>
 * 
 * <p>A "switch" is an argument preceded by one or two dashes that indicates a
 * Boolean status for some application behavior. A switch is {@code true} if it
 * was present as a token in the command line input, and is {@code false} if it
 * was absent.</p>
 * 
 * <p>A "parameter" is an argument preceded by one or two dashes where the next
 * token is that parameter's value. For example, the application's usage might
 * call for a {@code -f} parameter for "filename", where the next token on the
 * command line would be that filename. The processor would also interpret
 * "long-form" parameters like {@code --filename=foo}, where the parameter is
 * two dashes and a long tag, followed by an equal sign, and then the
 * value.</p>
 * 
 * <p>A "value" is any string token, obtained from the command line, for which
 * no preceding parameter was used. For a console application whose usage is of
 * the form "{@code doSomethingTo foo bar baz}", the three tokens {@code foo},
 * {@code bar}, and {@code baz} as values.</p>
 * 
 * <h3>Weak Synchronization of Inputs</h3>
 * 
 * <p>When actually invoked as a console application, the {@code main()} method
 * should simply call {@code processArgs()} with the original argument list.
 * From that point onward, member methods can act on the list of switches,
 * parameters, and values, that were parsed by that method.</p>
 * 
 * <p>When this class is consumed by other classes as a {@code Runnable} task,
 * however, the consumer may choose to simply set the inputs explicitly with
 * {@link #switchOn}, {@link #setParam}, and {@link #pushValue}. If these
 * methods are used to set the task's inputs, then the instance makes no attempt
 * to synchronize a fake list of original arguments. The consumer of the class
 * thus <i>must not</i> do anything that would call {@link #processArgs}, which
 * would act on the (probably empty) list of command-line arguments and wipe out
 * any explicitly set switches, parameters, and values that might already be
 * present.</p>
 */
public abstract class ConsoleApplication
implements Runnable
{
	/**
	 * The character used to mark a given command line argument as either a
	 * switch or a parameter. Following the *nix convention for command line
	 * arguments, this constant is set as the dash character ({@code '-'}).  
	 */
	public static final char ARG_MARKER = '-' ;
	
	/**
	 * String arguments received from the command line. These are not typically
	 * necessary for an instance of a {@link Runnable} class which could have
	 * its own member fields, but when using an implementation of
	 * {@code ConsoleApplication}, the consumer might choose to interact with
	 * it in a similar way &mdash; by passing in a list of command line
	 * arguments which are processed by the same algorithm that would be used
	 * from the console.
	 */
	protected String[] m_asArgs = null ;
	/** An array of command-line "switches". */
	protected ArrayList<String> m_asSwitches = null ;
	/** A map of command-line "parameter" arguments to their string values. */
	protected HashMap<String,String> m_mapParams = null ;
	/** An integer-to-string mapping of "values" */
	protected ArrayList<String> m_asValues = null ;
	/**
	 * A {@link Logger} instance for the application. If not explicitly set by
	 * the class's consumer, or when invoking this application from the
	 * console, an instance will be created with the implementation class's
	 * qualified name.
	 */
	protected Logger m_log = null ;
	
	/**
	 * Initializes all of the member fields that are provided concretely by
	 * this abstract class: an array of the original arguments, a map of
	 * switches, a map of parameters, and an array list of values.
	 * Implementation classes should override this with any other necessary
	 * initialization logic for members of that class.
	 * @return the app instance, for chained invocations
	 */
	protected ConsoleApplication init()
	{
		m_asArgs = null ;
		this.initSwitches().initParams().initValues().getLogger() ;
		return this ;
	}
	
	/**
	 * Initializes the list of "switches".
	 * @return the app instance, for chained invocations
	 */
	protected ConsoleApplication initSwitches()
	{
		if( m_asSwitches == null )
			m_asSwitches = new ArrayList<String>() ;
		else
			m_asSwitches.clear() ;
		return this ;
	}
	
	/**
	 * Initializes the map of "parameters" to their values.
	 * @return the app instance, for chained invocations
	 */
	protected ConsoleApplication initParams()
	{
		if( m_mapParams == null )
			m_mapParams = new HashMap<String,String>() ;
		else
			m_mapParams.clear() ;
		return this ;
	}
	
	/**
	 * Initializes the list of "values".
	 * @return the app instance, for chained invocations
	 */
	protected ConsoleApplication initValues()
	{
		if( m_asValues == null )
			m_asValues = new ArrayList<String>() ;
		else
			m_asValues.clear() ;
		return this ;
	}
	
	/**
	 * This class is designed with the intention that the actual execution of
	 * the application's main code occurs here, in an instance's {@code run()}
	 * method, rather than in the usual static {@code main(String[])} method.
	 * An implementation class should provide a concrete {@code run()} method
	 * which is called by {@code main()} after processing the arguments that
	 * have come in from the command line. 
	 * @return the app instance, for chained invocations
	 */
	@Override
	public abstract void run() ;
	
	/**
	 * Sets the instance's raw command line arguments. An implementation
	 * class's constructor could call this method to initialize the instance's
	 * command line arguments before executing the actual app logic. If the
	 * instance had any previous argument list set, then any switch, parameter,
	 * and value data that was parsed from that old list will be discarded. The
	 * method <i>will not</i>, however, automatically parse the <i>new</i> list;
	 * to do this, call {@link #processArgs()}.
	 * @param asArgs an array of command line arguments as they would have
	 *  appeared in an invocation from a console
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication setArgs( final String[] asArgs )
	{
		if( m_asArgs != null ) // Clear all previous parse data.
			this.initSwitches().initParams().initValues() ;
		m_asArgs = asArgs ;
		return this ;
	}
	
	/**
	 * Accesses a raw command line argument by numeric index.
	 * @param nIndex the index of the command line argument
	 * @return the argument at that index, or {@code null} if no arguments were
	 *  ever set.
	 * @throws ArrayIndexOutOfBoundsException if arguments were set but the
	 *  specified index is below zero or greater than the length of the
	 *  argument list
	 */
	public String getArg( final int nIndex )
	{
		if( m_asArgs == null ) return null ;
		if( nIndex < 0 || nIndex > m_asArgs.length )
			throw new ArrayIndexOutOfBoundsException() ;
		return m_asArgs[nIndex] ;
	}
	
	/**
	 * Processes all of the instance's command line arguments to sort them into
	 * the three usage type classes (switches, parameters, values). If no args
	 * were supplied, then the method cuts itself off as a no-op. The algorithm
	 * handles short-form switches ({@code -s}), long-form switches
	 * ({@code --switch}), short-form parameters ({@code -p value}), long-form
	 * parameters ({@code --parameter=value}), composite short-form switches in
	 * the vein of *nix commands like {@code tar} ({@code -hijklmnop value}),
	 * and naked values.
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication processArgs()
	{
		if( m_asArgs == null || m_asArgs.length == 0 )
			return this ;
		
		for( int i = 0 ; i < m_asArgs.length ; i++ )
		{
			final String sArg = m_asArgs[i] ;
			if( sArg.charAt( 0 ) == ARG_MARKER )
			{ // It is a switch or parameter.
				if( sArg.charAt( 1 ) == ARG_MARKER )
				{ // It is a long-form switch/parameter.
					if( sArg.contains( "=" ) )
					{ // It is a parameter of the form "--foo=value".
						String sParam = sArg.substring(2) ;
						String[] asParts = sParam.split( "\\=" ) ;
						if( asParts.length != 2 )
							this.logArgParseFailure( sArg ) ;
						else
							this.setParam( asParts[0], asParts[1] ) ;
					}
					else // It is a switch of the form "--foo".
						this.switchOn( sArg.substring(2) ) ;
				}
				else if( sArg.length() > 2 )
				{ // It's a chained series, possibly ending with parameter.
					for( int ci = 1 ; ci < sArg.length() - 1  ; ci++ )
						this.switchOn( sArg.charAt(ci) ) ;
					final char cLast = sArg.charAt( sArg.length()-1 ) ;
					if( i == m_asArgs.length - 1 ) // last arg in list
						this.switchOn( cLast ) ;
					else if( m_asArgs[i+1].charAt(0) == ARG_MARKER )
						this.switchOn( cLast ) ;
					else // add a parameter and advance the loop over its value
						this.setParam( cLast, m_asArgs[++i] ) ;
				}
				else
				{ // It's a short-form switch/parameter.
					final char cName = sArg.charAt(1) ;
					if( i == m_asArgs.length - 1 ) // last arg in list
						this.switchOn( cName ) ;
					else if( m_asArgs[i+1].charAt(0) == ARG_MARKER )
						this.switchOn( cName ) ;
					else // add a parameter and advance the loop over its value
						this.setParam( cName, m_asArgs[++i] ) ;
				}
			}
			else
			{ // It is a naked value.
				this.pushValue( sArg ) ;
			}
		}
		return this ;
	}
	
	/**
	 * Writes a warning to the log file, indicating that a parameter could not
	 * be parsed.
	 * @param sOffender the offending string
	 */
	protected void logArgParseFailure( final String sOffender )
	{
		this.getLogger().log( Level.WARNING,
			String.format( "Invalid parameter [%s]", sOffender ) ) ;
		return ;
	}
	
	/**
	 * Convenience method for chaining {@link #setArgs(String[])} and
	 * {@link #processArgs()}.
	 * @param asArgs an array of command line arguments as they would have
	 *  appeared in an invocation from a console
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication processArgs( final String[] asArgs )
	{ return this.setArgs(asArgs).processArgs() ; }
	
	/**
	 * Activates a "switch" by adding it to the list of found switches.
	 * If already in the list, this method is a no-op.
	 * @param sSwitch the switch to turn on
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication switchOn( final String sSwitch )
	{
		if( ! m_asSwitches.contains( sSwitch ) )
			m_asSwitches.add( sSwitch ) ;
		return this ;
	}
	
	/**
	 * @see #switchOn(String)
	 * @param cSwitch the switch to turn on
	 */
	public ConsoleApplication switchOn( final char cSwitch )
	{ return this.switchOn( Character.toString(cSwitch) ) ; }
	
	/**
	 * Deactivates a "switch" by removing it from the list of found switches.
	 * If already not in the list, this method is a no-op.
	 * @param sSwitch the switch to turn off
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication switchOff( final String sSwitch )
	{
		m_asSwitches.remove( sSwitch ) ;
		return this ;
	}
	
	/**
	 * @see #switchOff(String)
	 * @param cSwitch the switch to turn off
	 */
	public ConsoleApplication switchOff( final char cSwitch )
	{ return this.switchOff( Character.toString(cSwitch) ) ; }
	
	/**
	 * Indicates whether the specified "switch" has been turned on.
	 * @param sSwitch the name of the switch
	 * @return {@code true} if the switch is turned on
	 */
	public boolean getSwitch( final String sSwitch )
	{ return m_asSwitches.contains(sSwitch) ; }
	
	/**
	 * @see #getSwitch(String)
	 * @param cSwitch the name of the switch
	 */
	public boolean getSwitch( final char cSwitch )
	{ return m_asSwitches.contains( Character.toString(cSwitch) ) ; }
	
	/**
	 * Sets the value of a "parameter".
	 * Since the parameter list is backed by a hash map, this method will
	 * blindly overwrite any previously-set value of the parameter.
	 * @param sName the name of the parameter
	 * @param sValue the value of the parameter
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication setParam( final String sName,
		final String sValue )
	{
		m_mapParams.put( sName, sValue ) ;
		return this ;
	}
	
	/**
	 * @see #setParam(String,String)
	 * @param cName the name of the parameter
	 */
	public ConsoleApplication setParam( final char cName, final String sValue )
	{ return this.setParam( Character.toString(cName), sValue ) ; }
	
	/**
	 * Accesses the value of a "parameter".
	 * @param sName the name of the parameter
	 * @return the value of the parameter, or {@code null} if not set
	 */
	public String getParam( final String sName )
	{ return m_mapParams.get( sName ) ; }
	
	/**
	 * @see #getParam(String)
	 * @param cName the name of the parameter
	 */
	public String getParam( final char cName )
	{ return m_mapParams.get( Character.toString(cName) ) ; }
	
	/**
	 * Clears the value of a "parameter", if present.
	 * @param sName the name of the parameter
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication clearParam( final String sName )
	{
		m_mapParams.remove( sName ) ;
		return this ;
	}
	
	/**
	 * @param cName the name of the parameter
	 * @see #clearParam(String)
	 */
	public ConsoleApplication clearParam( final char cName )
	{ return this.clearParam( Character.toString(cName) ) ; }
	
	/**
	 * Pushes a "value" onto the end of the list of values. This is exposed in
	 * the interface as a "push" rather than an "add" to emphasize the fact that
	 * you're forced to push onto the end of the array, rather than being able
	 * to insert values at arbitrary indices. As a consequence of this, rather
	 * than returning the app instance to facilitate chaining (as other setter
	 * methods do), this method returns the index of the value you just pushed,
	 * so you can at least <i>access</i> the value by its index later. This
	 * method also differs from the other input-setting methods because it
	 * allows duplicate (non-unique) values.
	 * @param sValue the value to add
	 * @return the index of the value you just pushed
	 */
	public int pushValue( final String sValue )
	{
		m_asValues.add( sValue ) ;
		return m_asValues.size() - 1 ;
	}
	
	/**
	 * As an alternative to pushing the values individually, a consumer may set
	 * an explicit array of values all at once. This will <i>clear any previous
	 * list</i> and <i>replace</i> it with the list supplied in this call.
	 * @param asValues an array of values
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication setValues( final String[] asValues )
	{
		this.initValues() ;
		for( String s : asValues ) this.pushValue(s) ;
		return this ;
	}
	
	/**
	 * Accesses a "value" by index. 
	 * @param nIndex the index of the value to access
	 * @return the value at that index, or {@code null} if the instance has no
	 *  values (rather than throwing a WhatAShamefulLackOfMoralityException)
	 */
	public String getValue( final int nIndex )
	{
		if( m_asValues == null ) return null ;
		if( nIndex < 0 || nIndex > m_asValues.size() )
			throw new ArrayIndexOutOfBoundsException() ;
		return m_asValues.get(nIndex) ;
	}
	
	/**
	 * Gets the instance's logger. If not yet set, the method will initialize
	 * a logger using the instance's qualified class name as the logger name.
	 * @return the instance's logger
	 */
	public Logger getLogger()
	{
		if( m_log == null )
			m_log = Logger.getLogger( this.getClass().getCanonicalName() ) ;
		return m_log ;
	}
	
	/**
	 * Allows a consumer to set a custom logger for the application class.
	 * @param log a logger instance
	 * @return the app instance, for chained invocations
	 */
	public ConsoleApplication setLogger( Logger log )
	{ m_log = log ; return this ; }
}
