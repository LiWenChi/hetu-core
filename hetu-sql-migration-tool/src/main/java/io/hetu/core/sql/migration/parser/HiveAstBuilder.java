/*
 * Copyright (C) 2018-2020. Huawei Technologies Co., Ltd. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hetu.core.sql.migration.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.hetu.core.migration.source.hive.HiveSqlBaseVisitor;
import io.hetu.core.migration.source.hive.HiveSqlLexer;
import io.hetu.core.migration.source.hive.HiveSqlParser;
import io.hetu.core.sql.migration.tool.ConvertionOptions;
import io.prestosql.sql.parser.ParsingException;
import io.prestosql.sql.tree.AddColumn;
import io.prestosql.sql.tree.AliasedRelation;
import io.prestosql.sql.tree.AllColumns;
import io.prestosql.sql.tree.ArithmeticBinaryExpression;
import io.prestosql.sql.tree.ArithmeticUnaryExpression;
import io.prestosql.sql.tree.ArrayConstructor;
import io.prestosql.sql.tree.AssignmentItem;
import io.prestosql.sql.tree.BetweenPredicate;
import io.prestosql.sql.tree.BinaryLiteral;
import io.prestosql.sql.tree.BooleanLiteral;
import io.prestosql.sql.tree.Cast;
import io.prestosql.sql.tree.CharLiteral;
import io.prestosql.sql.tree.CoalesceExpression;
import io.prestosql.sql.tree.ColumnDefinition;
import io.prestosql.sql.tree.Comment;
import io.prestosql.sql.tree.ComparisonExpression;
import io.prestosql.sql.tree.CreateRole;
import io.prestosql.sql.tree.CreateSchema;
import io.prestosql.sql.tree.CreateTable;
import io.prestosql.sql.tree.CreateTableAsSelect;
import io.prestosql.sql.tree.CreateView;
import io.prestosql.sql.tree.Cube;
import io.prestosql.sql.tree.CurrentTime;
import io.prestosql.sql.tree.CurrentUser;
import io.prestosql.sql.tree.DecimalLiteral;
import io.prestosql.sql.tree.Delete;
import io.prestosql.sql.tree.DereferenceExpression;
import io.prestosql.sql.tree.DoubleLiteral;
import io.prestosql.sql.tree.DropRole;
import io.prestosql.sql.tree.DropSchema;
import io.prestosql.sql.tree.DropTable;
import io.prestosql.sql.tree.DropView;
import io.prestosql.sql.tree.Except;
import io.prestosql.sql.tree.ExistsPredicate;
import io.prestosql.sql.tree.Explain;
import io.prestosql.sql.tree.Expression;
import io.prestosql.sql.tree.Extract;
import io.prestosql.sql.tree.FrameBound;
import io.prestosql.sql.tree.FunctionCall;
import io.prestosql.sql.tree.GenericLiteral;
import io.prestosql.sql.tree.Grant;
import io.prestosql.sql.tree.GrantRoles;
import io.prestosql.sql.tree.GroupBy;
import io.prestosql.sql.tree.GroupingElement;
import io.prestosql.sql.tree.GroupingOperation;
import io.prestosql.sql.tree.GroupingSets;
import io.prestosql.sql.tree.Identifier;
import io.prestosql.sql.tree.IfExpression;
import io.prestosql.sql.tree.InListExpression;
import io.prestosql.sql.tree.InPredicate;
import io.prestosql.sql.tree.Insert;
import io.prestosql.sql.tree.Intersect;
import io.prestosql.sql.tree.IntervalLiteral;
import io.prestosql.sql.tree.IsNotNullPredicate;
import io.prestosql.sql.tree.IsNullPredicate;
import io.prestosql.sql.tree.Join;
import io.prestosql.sql.tree.JoinCriteria;
import io.prestosql.sql.tree.JoinOn;
import io.prestosql.sql.tree.LikeClause;
import io.prestosql.sql.tree.LikePredicate;
import io.prestosql.sql.tree.Limit;
import io.prestosql.sql.tree.LogicalBinaryExpression;
import io.prestosql.sql.tree.LongLiteral;
import io.prestosql.sql.tree.Node;
import io.prestosql.sql.tree.NodeLocation;
import io.prestosql.sql.tree.NotExpression;
import io.prestosql.sql.tree.NullIfExpression;
import io.prestosql.sql.tree.NullLiteral;
import io.prestosql.sql.tree.Offset;
import io.prestosql.sql.tree.OrderBy;
import io.prestosql.sql.tree.Parameter;
import io.prestosql.sql.tree.PathElement;
import io.prestosql.sql.tree.PathSpecification;
import io.prestosql.sql.tree.PrincipalSpecification;
import io.prestosql.sql.tree.Property;
import io.prestosql.sql.tree.QualifiedName;
import io.prestosql.sql.tree.QuantifiedComparisonExpression;
import io.prestosql.sql.tree.Query;
import io.prestosql.sql.tree.QueryBody;
import io.prestosql.sql.tree.QuerySpecification;
import io.prestosql.sql.tree.Relation;
import io.prestosql.sql.tree.RenameTable;
import io.prestosql.sql.tree.Revoke;
import io.prestosql.sql.tree.RevokeRoles;
import io.prestosql.sql.tree.Rollup;
import io.prestosql.sql.tree.Row;
import io.prestosql.sql.tree.SampledRelation;
import io.prestosql.sql.tree.SearchedCaseExpression;
import io.prestosql.sql.tree.Select;
import io.prestosql.sql.tree.SelectItem;
import io.prestosql.sql.tree.SetRole;
import io.prestosql.sql.tree.ShowColumns;
import io.prestosql.sql.tree.ShowCreate;
import io.prestosql.sql.tree.ShowFunctions;
import io.prestosql.sql.tree.ShowGrants;
import io.prestosql.sql.tree.ShowRoles;
import io.prestosql.sql.tree.ShowSchemas;
import io.prestosql.sql.tree.ShowSession;
import io.prestosql.sql.tree.ShowTables;
import io.prestosql.sql.tree.SimpleCaseExpression;
import io.prestosql.sql.tree.SimpleGroupBy;
import io.prestosql.sql.tree.SingleColumn;
import io.prestosql.sql.tree.SortItem;
import io.prestosql.sql.tree.Statement;
import io.prestosql.sql.tree.StringLiteral;
import io.prestosql.sql.tree.SubqueryExpression;
import io.prestosql.sql.tree.SubscriptExpression;
import io.prestosql.sql.tree.Table;
import io.prestosql.sql.tree.TableElement;
import io.prestosql.sql.tree.TableSubquery;
import io.prestosql.sql.tree.TimeLiteral;
import io.prestosql.sql.tree.TimestampLiteral;
import io.prestosql.sql.tree.Union;
import io.prestosql.sql.tree.Update;
import io.prestosql.sql.tree.Use;
import io.prestosql.sql.tree.Values;
import io.prestosql.sql.tree.WhenClause;
import io.prestosql.sql.tree.Window;
import io.prestosql.sql.tree.WindowFrame;
import io.prestosql.sql.tree.With;
import io.prestosql.sql.tree.WithQuery;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class HiveAstBuilder
        extends HiveSqlBaseVisitor<Node>
{
    private static final Map<String, String> HIVE_TO_HETU_FILE_FORMAT = new HashMap<String, String>() {
        {
            put("SEQUENCEFILE", "SequenceFile");
            put("TEXTFILE", "TextFile");
            put("RCFILE", "RCText");
            put("ORC", "ORC");
            put("PARQUET", "Parquet");
            put("AVRO", "Avro");
            put("JSONFILE", "JSON");
        }
    };

    private int parameterPosition;
    private final ConvertionOptions convertionOptions;

    private List<String> conversionInfo = new ArrayList<>();

    public List<String> getConversionInfo()
    {
        return conversionInfo;
    }

    public HiveAstBuilder(ConvertionOptions convertionOptions)
    {
        this.convertionOptions = requireNonNull(convertionOptions, "convertionOptions is null");
    }

    @Override
    public Node visitSingleStatement(HiveSqlParser.SingleStatementContext context)
    {
        return visit(context.statement());
    }

    @Override
    public Node visitStandaloneExpression(HiveSqlParser.StandaloneExpressionContext context)
    {
        return visit(context.expression());
    }

    @Override
    public Node visitStandalonePathSpecification(HiveSqlParser.StandalonePathSpecificationContext context)
    {
        return visit(context.pathSpecification());
    }

    // ******************* statements **********************
    @Override
    public Node visitUse(HiveSqlParser.UseContext context)
    {
        return new Use(getLocation(context), Optional.empty(), (Identifier) visit(context.schema));
    }

    @Override
    public Node visitCreateSchema(HiveSqlParser.CreateSchemaContext context)
    {
        if (context.DBPROPERTIES() != null) {
            throw parseError("Unsupported attribute: DBPROPERTIES", context.properties());
        }

        List<Property> properties = new ArrayList<>();
        if (context.COMMENT() != null) {
            Expression expression = (StringLiteral) visit(context.comment);
            conversionInfo.add(format("COMMENT: %s", expression.toString()));
        }

        if (context.LOCATION() != null) {
            Identifier identifier = new Identifier("location");
            Expression expression = (StringLiteral) visit(context.location);
            properties.add(new Property(getLocation(context), identifier, expression));
        }

        return new CreateSchema(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                context.EXISTS() != null,
                properties);
    }

    @Override
    public Node visitDropSchema(HiveSqlParser.DropSchemaContext context)
    {
        if (context.CASCADE() != null) {
            throw parseError("Unsupported statement: CASCADE", context);
        }

        return new DropSchema(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                context.EXISTS() != null, false);
    }

    @Override
    public Node visitShowSchemas(HiveSqlParser.ShowSchemasContext context)
    {
        Optional<String> pattern = getTextIfPresent(context.pattern).map(HiveAstBuilder::unquote);
        Optional<String> escape = Optional.empty();
        if (pattern.isPresent()) {
            if (pattern.get().contains("|")) {
                throw parseError("Unsupported wildcards: |", context.pattern);
            }
            if (pattern.get().contains("_")) {
                pattern = Optional.of(pattern.get().replace("_", "#_"));
                escape = Optional.of("#");
            }
            pattern = Optional.of(pattern.get().replace("*", "%"));
        }
        return new ShowSchemas(getLocation(context), Optional.empty(), pattern, escape);
    }

    @Override
    public Node visitAlterSchema(HiveSqlParser.AlterSchemaContext context)
    {
        throw parseError("Unsupported statement: Alter Schema/Database", context);
    }

    @Override
    public Node visitDescribeSchema(HiveSqlParser.DescribeSchemaContext context)
    {
        throw parseError("Unsupported statement: Describe Schema/Database", context);
    }

    @Override
    public Node visitCreateView(HiveSqlParser.CreateViewContext context)
    {
        if (context.COMMENT() != null) {
            Expression expression = (Expression) visit(context.string());
            conversionInfo.add(format("COMMENT: %s", expression.toString()));
        }

        if (context.viewColumns() != null) {
            throw parseError("Unsupported attribute: COLUMN ALIASES", context.viewColumns());
        }

        if (context.TBLPROPERTIES() != null) {
            throw parseError("Unsupported attribute: TBLPROPERTIES", context.properties());
        }

        return new CreateView(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Query) visit(context.query()),
                false,
                Optional.empty());
    }

    @Override
    public Node visitAlterView(HiveSqlParser.AlterViewContext context)
    {
        if (context.TBLPROPERTIES() != null) {
            throw parseError("Unsupported attribute: TBLPROPERTIES", context.properties());
        }

        return new CreateView(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Query) visit(context.query()),
                true,
                Optional.empty());
    }

    @Override
    public Node visitShowViews(HiveSqlParser.ShowViewsContext context)
    {
        throw parseError("Unsupported statement: Show Views", context);
    }

    @Override
    public Node visitDropView(HiveSqlParser.DropViewContext context)
    {
        return new DropView(getLocation(context), getQualifiedName(context.qualifiedName()), context.EXISTS() != null);
    }

    @Override
    public Node visitCreateTable(HiveSqlParser.CreateTableContext context)
    {
        if (context.TEMPORARY() != null) {
            throw parseError("Unsupported statement: CREATE TEMPORARY TABLE", context);
        }
        if (context.constraintSpecification() != null) {
            throw parseError("Unsupported constraint statement", context.constraintSpecification());
        }

        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string(0))).getValue());
        }

        List<Property> properties = new ArrayList<>();
        if (context.TRANSACTIONAL() != null) {
            Identifier name = new Identifier("transactional");
            Expression value = new Identifier("true");
            properties.add(new Property(name, value));
        }
        List<TableElement> elements = getTableElements(context.tableElement());
        if (context.PARTITIONED() != null) {
            Identifier name = new Identifier("partitioned_by");
            List<ColumnDefinition> columnDefinitions = getColumnDefinitions(context.partitionedBy().columnDefinition());

            List<Expression> expressions = new ArrayList<>();
            Iterator<ColumnDefinition> iterator = columnDefinitions.iterator();
            while (iterator.hasNext()) {
                ColumnDefinition iter = iterator.next();
                expressions.add(new StringLiteral(iter.getName().getValue()));
                elements.add(new ColumnDefinition(iter.getName(), iter.getType(), true, emptyList(), Optional.empty()));
            }
            Expression value = new ArrayConstructor(expressions);
            properties.add(new Property(name, value));
        }
        if (context.CLUSTERED() != null) {
            Identifier name = new Identifier("bucketed_by");
            List<Expression> quotedExpressions = new ArrayList<>();
            List<Expression> expressions = getExpressions(context.clusteredBy().expression());
            for (int i = 0; i < expressions.size(); i++) {
                quotedExpressions.add(new StringLiteral(expressions.get(i).toString()));
            }
            Expression value = new ArrayConstructor(quotedExpressions);
            properties.add(new Property(name, value));
        }
        if (context.SORTED() != null) {
            Identifier name = new Identifier("sorted_by");
            List<Expression> expressions = new ArrayList<>();
            List<HiveSqlParser.SortItemContext> sortItemContexts = context.sortedBy().sortItem();
            for (int i = 0; i < sortItemContexts.size(); i++) {
                HiveSqlParser.SortItemContext sortItemContext = sortItemContexts.get(i);
                String sortedBy = sortItemContext.expression().getText();
                if (sortItemContext.ordering != null) {
                    sortedBy += " " + sortItemContext.ordering.getText();
                }
                expressions.add(new StringLiteral(sortedBy));
            }

            Expression value = new ArrayConstructor(expressions);
            properties.add(new Property(name, value));
        }
        if (context.INTO() != null) {
            Identifier name = new Identifier("bucket_count");
            Expression value = (Expression) visit(context.bucketcount);
            properties.add(new Property(name, value));
        }
        if (context.SKEWED() != null) {
            throw parseError("Unsupported statement: SKEWED", context.columnAliases());
        }
        if (context.ROW() != null) {
            throw parseError("Unsupported statement: ROW FORMAT", context.rowFormat());
        }
        if (context.STORED() != null) {
            throw parseError("Unsupported statement: STORED BY", context.storedBy);
        }
        if (context.STORED_AS().size() != 0) {
            Identifier name = new Identifier("format");
            String storedAsString = ((Identifier) visit(context.stored_as)).getValue();
            Expression value = new StringLiteral(getFileFormat(storedAsString));
            properties.add(new Property(name, value));
        }
        if (context.EXTERNAL() != null) {
            if (context.LOCATION() == null) {
                throw parseError("Unsupported statement: External attribute should be used with location", context);
            }
            Identifier name = new Identifier("external");
            Expression value = new Identifier("true");
            properties.add(new Property(name, value));
        }
        if (context.LOCATION() != null) {
            Identifier name = new Identifier("location");
            Expression value = (StringLiteral) visit(context.location);
            properties.add(new Property(name, value));
        }
        if (context.TBLPROPERTIES() != null) {
            List<Property> tblProperties = visit(context.tableProperties.property(), Property.class);
            for (int i = 0; i < tblProperties.size(); i++) {
                Property property = tblProperties.get(i);
                if (property.getName().getValue().equalsIgnoreCase("transactional")) {
                    Identifier name = new Identifier("transactional");
                    Expression value = new Identifier(unquote(property.getValue().toString()));
                    properties.add(new Property(name, value));
                }
                else {
                    throw parseError(format("Unsupported attribute: %s", property.getName().getValue()), context.tableProperties);
                }
            }
        }

        return new CreateTable(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                elements,
                context.EXISTS() != null,
                properties,
                comment);
    }

    @Override
    public Node visitCreateTableAsSelect(HiveSqlParser.CreateTableAsSelectContext context)
    {
        if (context.TEMPORARY() != null) {
            throw parseError("Unsupported statement: CREATE TEMPORARY TABLE", context);
        }

        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string(0))).getValue());
        }

        Optional<List<Identifier>> columnAliases = Optional.empty();
        if (context.columnAliases() != null) {
            columnAliases = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        List<Property> properties = new ArrayList<>();
        if (context.TRANSACTIONAL() != null) {
            Identifier name = new Identifier("transactional");
            Expression value = new Identifier("true");
            properties.add(new Property(name, value));
        }
        if (context.STORED_AS() != null) {
            Identifier name = new Identifier("format");
            String storedAsString = ((Identifier) visit(context.stored_as)).getValue();
            Expression value = new StringLiteral(getFileFormat(storedAsString));
            properties.add(new Property(name, value));
        }
        if (context.LOCATION() != null) {
            Identifier name = new Identifier("location");
            Expression value = (StringLiteral) visit(context.location);
            properties.add(new Property(name, value));
        }
        if (context.TBLPROPERTIES() != null) {
            List<Property> tableProperties = visit(context.properties().property(), Property.class);
            for (int i = 0; i < tableProperties.size(); i++) {
                Property property = tableProperties.get(i);
                if (property.getName().getValue().equalsIgnoreCase("transactional")) {
                    Identifier name = new Identifier("transactional");
                    Expression value = new Identifier(unquote(property.getValue().toString()));
                    properties.add(new Property(name, value));
                }
                else {
                    throw parseError(format("Unsupported attribute: %s", property.getName().getValue()), context.properties());
                }
            }
        }

        return new CreateTableAsSelect(
                getLocation(context),
                getQualifiedName(context.qualifiedName()),
                (Query) visit(context.query()),
                context.EXISTS() != null,
                properties,
                true,
                columnAliases,
                comment);
    }

    @Override
    public Node visitCreateTableLike(HiveSqlParser.CreateTableLikeContext context)
    {
        List<TableElement> elements = new ArrayList<>();
        LikeClause likeClause = new LikeClause(getQualifiedName(context.likeTableName), Optional.of(LikeClause.PropertiesOption.EXCLUDING));
        elements.add(likeClause);

        List<Property> properties = new ArrayList<>();
        if (context.EXTERNAL() != null) {
            if (context.LOCATION() == null) {
                throw parseError("External attribute should be used with location", context);
            }
            Identifier name = new Identifier("external");
            Expression value = new Identifier("true");
            properties.add(new Property(name, value));
        }
        if (context.LOCATION() != null) {
            Identifier name = new Identifier("location");
            Expression value = (StringLiteral) visit(context.location);
            properties.add(new Property(name, value));
        }

        return new CreateTable(
                getLocation(context),
                getQualifiedName(context.tableName),
                elements,
                context.EXISTS() != null,
                properties,
                Optional.empty());
    }

    @Override
    public Node visitShowTables(HiveSqlParser.ShowTablesContext context)
    {
        Optional<String> pattern = getTextIfPresent(context.pattern).map(HiveAstBuilder::unquote);
        Optional<String> escape = Optional.empty();
        if (pattern.isPresent()) {
            if (pattern.get().contains("|")) {
                throw parseError("Unsupported wildcards: |", context.pattern);
            }
            if (pattern.get().contains("_")) {
                pattern = Optional.of(pattern.get().replace("_", "#_"));
                escape = Optional.of("#");
            }
            pattern = Optional.of(pattern.get().replace("*", "%"));
        }
        return new ShowTables(
                getLocation(context),
                Optional.ofNullable(context.qualifiedName())
                        .map(this::getQualifiedName),
                pattern,
                escape);
    }

    @Override
    public Node visitShowCreateTable(HiveSqlParser.ShowCreateTableContext context)
    {
        conversionInfo.add(format("SHOW CREATE VIEW %s", getQualifiedName(context.qualifiedName())));
        return new ShowCreate(getLocation(context), ShowCreate.Type.TABLE, getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitRenameTable(HiveSqlParser.RenameTableContext context)
    {
        return new RenameTable(getLocation(context), getQualifiedName(context.from), getQualifiedName(context.to));
    }

    @Override
    public Node visitCommentTable(HiveSqlParser.CommentTableContext context)
    {
        Optional<String> comment = Optional.empty();
        List<Property> tableProperties = visit(context.properties().property(), Property.class);
        for (int i = 0; i < tableProperties.size(); i++) {
            Property property = tableProperties.get(i);
            if (property.getName().getValue().equalsIgnoreCase("comment")) {
                comment = Optional.of(unquote(property.getValue().toString()));
            }
            else {
                throw parseError(format("Unsupported attribute: %s", property.getName().getValue()), context.properties());
            }
        }

        return new Comment(getLocation(context), Comment.Type.TABLE, getQualifiedName(context.qualifiedName()), comment);
    }

    @Override
    public Node visitAlterTableAddConstraint(HiveSqlParser.AlterTableAddConstraintContext context)
    {
        throw parseError("Unsupported statement: Alter Table Add Constraint", context);
    }

    @Override
    public Node visitAlterTableChangeConstraint(HiveSqlParser.AlterTableChangeConstraintContext context)
    {
        throw parseError("Unsupported statement: Alter Table Change Constraint", context);
    }

    @Override
    public Node visitAlterTableDropConstraint(HiveSqlParser.AlterTableDropConstraintContext context)
    {
        throw parseError("Unsupported statement: Alter Table Drop Constraint", context);
    }

    @Override
    public Node visitAlterTableSerde(HiveSqlParser.AlterTableSerdeContext context)
    {
        throw parseError("Unsupported statement: Alter Table Serde", context);
    }

    @Override
    public Node visitAlterTableStorage(HiveSqlParser.AlterTableStorageContext context)
    {
        throw parseError("Unsupported statement: Alter Table Storage", context);
    }

    @Override
    public Node visitAlterTableSkewed(HiveSqlParser.AlterTableSkewedContext context)
    {
        throw parseError("Unsupported statement: Alter Table Skewed", context);
    }

    @Override
    public Node visitAlterTableNotSkewed(HiveSqlParser.AlterTableNotSkewedContext context)
    {
        throw parseError("Unsupported statement: Alter Table Not Skewed", context);
    }

    @Override
    public Node visitAlterTableNotAsDirectories(HiveSqlParser.AlterTableNotAsDirectoriesContext context)
    {
        throw parseError("Unsupported statement: Alter Table Not Stored As Directoriese", context);
    }

    @Override
    public Node visitAlterTableSetSkewedLocation(HiveSqlParser.AlterTableSetSkewedLocationContext context)
    {
        throw parseError("Unsupported statement: Alter Table Set Skewed Location", context);
    }

    @Override
    public Node visitAlterTableAddPartition(HiveSqlParser.AlterTableAddPartitionContext context)
    {
        throw parseError("Unsupported statement: Alter Table Add Partition", context);
    }

    @Override
    public Node visitAlterTableRenamePartition(HiveSqlParser.AlterTableRenamePartitionContext context)
    {
        throw parseError("Unsupported statement: Alter Table Rename Partition", context);
    }

    @Override
    public Node visitAlterTableExchangePartition(HiveSqlParser.AlterTableExchangePartitionContext context)
    {
        throw parseError("Unsupported statement: Alter Table Exchange Partition", context);
    }

    @Override
    public Node visitAlterTableRecoverPartitions(HiveSqlParser.AlterTableRecoverPartitionsContext context)
    {
        throw parseError("Unsupported statement: Alter Table Recover Partition", context);
    }

    @Override
    public Node visitAlterTableDropPartition(HiveSqlParser.AlterTableDropPartitionContext context)
    {
        throw parseError("Unsupported statement: Alter Table Drop Partition", context);
    }

    @Override
    public Node visitAlterTableArchivePartition(HiveSqlParser.AlterTableArchivePartitionContext context)
    {
        throw parseError("Unsupported statement: Alter Table Archive/Unarchive Partition", context);
    }

    @Override
    public Node visitAlterTablePartitionFileFormat(HiveSqlParser.AlterTablePartitionFileFormatContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition File Format", context);
    }

    @Override
    public Node visitAlterTablePartitionLocation(HiveSqlParser.AlterTablePartitionLocationContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition Location", context);
    }

    @Override
    public Node visitAlterTablePartitionTouch(HiveSqlParser.AlterTablePartitionTouchContext context)
    {
        throw parseError("Unsupported statement: Alter Table Touch Partition", context);
    }

    @Override
    public Node visitAlterTablePartitionProtections(HiveSqlParser.AlterTablePartitionProtectionsContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition Protections", context);
    }

    @Override
    public Node visitAlterTablePartitionCompact(HiveSqlParser.AlterTablePartitionCompactContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition Compact", context);
    }

    @Override
    public Node visitAlterTablePartitionConcatenate(HiveSqlParser.AlterTablePartitionConcatenateContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition Concatenate", context);
    }

    @Override
    public Node visitAlterTablePartitionUpdateColumns(HiveSqlParser.AlterTablePartitionUpdateColumnsContext context)
    {
        throw parseError("Unsupported statement: Alter Table Partition Update Columns", context);
    }

    @Override
    public Node visitAlterTableChangeColumn(HiveSqlParser.AlterTableChangeColumnContext context)
    {
        throw parseError("Unsupported statement: Alter Table Change Column", context);
    }

    @Override
    public Node visitShowTableExtended(HiveSqlParser.ShowTableExtendedContext context)
    {
        throw parseError("Unsupported statement: Show Table Extended", context);
    }

    @Override
    public Node visitShowTableProperties(HiveSqlParser.ShowTablePropertiesContext context)
    {
        throw parseError("Unsupported statement: Show Table Properties", context);
    }

    @Override
    public Node visitTruncateTable(HiveSqlParser.TruncateTableContext context)
    {
        throw parseError("Unsupported statement: Truncate Table", context);
    }

    @Override
    public Node visitMsckRepairTable(HiveSqlParser.MsckRepairTableContext context)
    {
        throw parseError("Unsupported statement: Msck Repair Table", context);
    }

    @Override
    public Node visitCreateMaterializedView(HiveSqlParser.CreateMaterializedViewContext context)
    {
        throw parseError("Unsupported statement: Create Materialized View", context);
    }

    @Override
    public Node visitDropMaterializedView(HiveSqlParser.DropMaterializedViewContext context)
    {
        throw parseError("Unsupported statement: Drop Materialized View", context);
    }

    @Override
    public Node visitAlterMaterializedView(HiveSqlParser.AlterMaterializedViewContext context)
    {
        throw parseError("Unsupported statement: Alter Materialized View", context);
    }

    @Override
    public Node visitShowMaterializedViews(HiveSqlParser.ShowMaterializedViewsContext context)
    {
        throw parseError("Unsupported statement: Show Materialized Views", context);
    }

    @Override
    public Node visitCreateFunction(HiveSqlParser.CreateFunctionContext context)
    {
        throw parseError("Unsupported statement: Create Function", context);
    }

    @Override
    public Node visitDropFunction(HiveSqlParser.DropFunctionContext context)
    {
        throw parseError("Unsupported statement: Drop Function", context);
    }

    @Override
    public Node visitReloadFunctions(HiveSqlParser.ReloadFunctionsContext context)
    {
        throw parseError("Unsupported statement: Reload Functions", context);
    }

    @Override
    public Node visitCreateIndex(HiveSqlParser.CreateIndexContext context)
    {
        throw parseError("Unsupported statement: Create Index", context);
    }

    @Override
    public Node visitDropIndex(HiveSqlParser.DropIndexContext context)
    {
        throw parseError("Unsupported statement: Drop Index", context);
    }

    @Override
    public Node visitAlterIndex(HiveSqlParser.AlterIndexContext context)
    {
        throw parseError("Unsupported statement: Alter Index", context);
    }

    @Override
    public Node visitShowIndex(HiveSqlParser.ShowIndexContext context)
    {
        throw parseError("Unsupported statement: Show Index", context);
    }

    @Override
    public Node visitShowPartitions(HiveSqlParser.ShowPartitionsContext context)
    {
        throw parseError("Unsupported statement: Show Partitions", context);
    }

    @Override
    public Node visitDescribePartition(HiveSqlParser.DescribePartitionContext context)
    {
        throw parseError("Unsupported statement: Describe Partition", context);
    }

    @Override
    public Node visitDescribeFunction(HiveSqlParser.DescribeFunctionContext context)
    {
        throw parseError("Unsupported statement: Describe Function", context);
    }

    @Override
    public Node visitCreateMacro(HiveSqlParser.CreateMacroContext context)
    {
        throw parseError("Unsupported statement: Create Macro", context);
    }

    @Override
    public Node visitDropMacro(HiveSqlParser.DropMacroContext context)
    {
        throw parseError("Unsupported statement: Drop Macro", context);
    }

    @Override
    public Node visitShowRoleGrant(HiveSqlParser.ShowRoleGrantContext context)
    {
        throw parseError("Unsupported statement: Show Role Grant", context);
    }

    @Override
    public Node visitShowPrincipals(HiveSqlParser.ShowPrincipalsContext context)
    {
        throw parseError("Unsupported statement: Show Principals", context);
    }

    @Override
    public Node visitShowLocks(HiveSqlParser.ShowLocksContext context)
    {
        throw parseError("Unsupported statement: Show Locks", context);
    }

    @Override
    public Node visitShowConf(HiveSqlParser.ShowConfContext context)
    {
        throw parseError("Unsupported statement: Show Conf", context);
    }

    @Override
    public Node visitShowTransactions(HiveSqlParser.ShowTransactionsContext context)
    {
        throw parseError("Unsupported statement: Show Transactions", context);
    }

    @Override
    public Node visitShowCompactions(HiveSqlParser.ShowCompactionsContext context)
    {
        throw parseError("Unsupported statement: Show Compactions", context);
    }

    @Override
    public Node visitAbortTransactions(HiveSqlParser.AbortTransactionsContext context)
    {
        throw parseError("Unsupported statement: Abort Transactions", context);
    }

    @Override
    public Node visitLoadData(HiveSqlParser.LoadDataContext context)
    {
        throw parseError("Unsupported statement: Load Data", context);
    }

    @Override
    public Node visitMerge(HiveSqlParser.MergeContext context)
    {
        throw parseError("Unsupported statement: Merge", context);
    }

    @Override
    public Node visitExportData(HiveSqlParser.ExportDataContext context)
    {
        throw parseError("Unsupported statement: Export", context);
    }

    @Override
    public Node visitImportData(HiveSqlParser.ImportDataContext context)
    {
        throw parseError("Unsupported statement: Import", context);
    }

    @Override
    public Node visitDropTable(HiveSqlParser.DropTableContext context)
    {
        if (context.PURGE() != null) {
            throw parseError("Unsupported attribute: PURGE", context);
        }
        return new DropTable(getLocation(context), getQualifiedName(context.qualifiedName()), context.EXISTS() != null);
    }

    @Override
    public Node visitShowColumns(HiveSqlParser.ShowColumnsContext context)
    {
        if (context.dbName != null) {
            throw parseError("Unsupported attribute: FROM/IN DB", context.dbName);
        }

        if (context.pattern != null) {
            throw parseError("Unsupported attribute: LIKE", context.pattern);
        }

        QualifiedName qualifiedName = QualifiedName.of(visit(context.tableName.identifier(), Identifier.class));
        return new ShowColumns(getLocation(context), qualifiedName);
    }

    @Override
    public Node visitDescribeTable(HiveSqlParser.DescribeTableContext context)
    {
        if (context.EXTENDED() != null || context.FORMATTED() != null) {
            throw parseError("Unsupported attribute: EXTENDED/FORMATTED", context);
        }

        if (context.describeTableOption() != null) {
            throw parseError("Unsupported Describe statement", context.describeTableOption());
        }

        QualifiedName qualifiedName = QualifiedName.of(visit(context.describeName().identifier(), Identifier.class));
        return new ShowColumns(getLocation(context), qualifiedName);
    }

    @Override
    public Node visitAddReplaceColumn(HiveSqlParser.AddReplaceColumnContext context)
    {
        if (context.CASCADE() != null) {
            throw parseError("Unsupported statement: CASCADE", context);
        }
        if (context.REPLACE() != null) {
            throw parseError("Unsupported statement: REPLACE", context);
        }
        if (context.PARTITION() != null) {
            throw parseError("Unsupported statement: PARTITION", context.partition);
        }
        if (context.tableElement().size() > 1) {
            throw parseError("Unsupported add multiple columns", context.tableElement(0));
        }

        HiveSqlParser.ColumnDefinitionContext columnDefinitionContext = context.tableElement(0).columnDefinition();
        Identifier identifier = (Identifier) visit(columnDefinitionContext.identifier());
        String type = getType(columnDefinitionContext.type());
        Optional<String> comment = Optional.empty();
        if (columnDefinitionContext.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(columnDefinitionContext.string())).getValue());
        }
        ColumnDefinition columnDefinition = new ColumnDefinition(identifier, type, true, ImmutableList.of(), comment);

        return new AddColumn(getLocation(context), getQualifiedName(context.qualifiedName()), columnDefinition);
    }

    @Override
    public Node visitInsertInto(HiveSqlParser.InsertIntoContext context)
    {
        if (context.PARTITION() != null) {
            throw parseError("Unsupported attribute: PARTITION", context.insertPartition());
        }

        Optional<List<Identifier>> columnAliases = Optional.empty();
        if (context.columnAliases() != null) {
            columnAliases = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        return new Insert(
                getQualifiedName(context.qualifiedName()),
                columnAliases,
                (Query) visit(context.query()));
    }

    @Override public Node visitInsertOverwrite(HiveSqlParser.InsertOverwriteContext context)
    {
        if (context.PARTITION() != null) {
            throw parseError("Unsupported attribute: PARTITION", context.insertPartition());
        }

        Optional<List<Identifier>> columnAliases = Optional.empty();
        return new Insert(
                getQualifiedName(context.qualifiedName()),
                columnAliases,
                (Query) visit(context.query()),
                true);
    }

    @Override
    public Node visitInsertFilesystem(HiveSqlParser.InsertFilesystemContext context)
    {
        throw parseError("Unsupported statement: Insert Filesystem", context);
    }

    @Override public Node visitUpdateTable(HiveSqlParser.UpdateTableContext context)
    {
        List<AssignmentItem> assignmentItems = ImmutableList.of();
        if (context.assignmentList() != null) {
            assignmentItems = visit(context.assignmentList().assignmentItem(), AssignmentItem.class);
        }
        return new Update(
                getLocation(context),
                new Table(getLocation(context), getQualifiedName(context.qualifiedName())),
                assignmentItems,
                visitIfPresent(context.booleanExpression(), Expression.class));
    }

    @Override
    public Node visitShowFunctions(HiveSqlParser.ShowFunctionsContext context)
    {
        if (context.LIKE() != null) {
            throw parseError("Unsupported attribute: LIKE", context.pattern);
        }

        return new ShowFunctions(getLocation(context));
    }

    @Override
    public Node visitCreateRole(HiveSqlParser.CreateRoleContext context)
    {
        return new CreateRole(
                getLocation(context),
                (Identifier) visit(context.name),
                Optional.empty());
    }

    @Override
    public Node visitDropRole(HiveSqlParser.DropRoleContext context)
    {
        return new DropRole(
                getLocation(context),
                (Identifier) visit(context.name));
    }

    @Override
    public Node visitGrantRoles(HiveSqlParser.GrantRolesContext context)
    {
        return new GrantRoles(
                getLocation(context),
                ImmutableSet.copyOf(getIdentifiers(context.roles().identifier())),
                ImmutableSet.copyOf(getPrincipalSpecifications(context.principal())),
                context.OPTION() != null,
                Optional.empty());
    }

    @Override
    public Node visitRevokeRoles(HiveSqlParser.RevokeRolesContext context)
    {
        return new RevokeRoles(
                getLocation(context),
                ImmutableSet.copyOf(getIdentifiers(context.roles().identifier())),
                ImmutableSet.copyOf(getPrincipalSpecifications(context.principal())),
                context.OPTION() != null,
                Optional.empty());
    }

    @Override
    public Node visitSetRole(HiveSqlParser.SetRoleContext context)
    {
        SetRole.Type type = SetRole.Type.ROLE;
        if (context.ALL() != null) {
            type = SetRole.Type.ALL;
        }
        else if (context.NONE() != null) {
            type = SetRole.Type.NONE;
        }
        return new SetRole(getLocation(context), type, getIdentifierIfPresent(context.role));
    }

    @Override
    public Node visitShowRoles(HiveSqlParser.ShowRolesContext context)
    {
        return new ShowRoles(
                getLocation(context),
                Optional.empty(),
                context.CURRENT() != null);
    }

    @Override
    public Node visitGrant(HiveSqlParser.GrantContext context)
    {
        Optional<List<String>> privileges = Optional.of(context.privilege().stream()
                    .map(HiveSqlParser.PrivilegeContext::getText)
                    .collect(toList()));
        return new Grant(
                getLocation(context),
                privileges,
                true,
                getQualifiedName(context.qualifiedName()),
                getPrincipalSpecification(context.grantee),
                context.OPTION() != null);
    }

    @Override
    public Node visitRevoke(HiveSqlParser.RevokeContext context)
    {
        Optional<List<String>> privileges = Optional.of(context.privilege().stream()
                    .map(HiveSqlParser.PrivilegeContext::getText)
                    .collect(toList()));
        return new Revoke(
                getLocation(context),
                context.OPTION() != null,
                privileges,
                true,
                getQualifiedName(context.qualifiedName()),
                getPrincipalSpecification(context.grantee));
    }

    @Override
    public Node visitShowGrants(HiveSqlParser.ShowGrantsContext context)
    {
        if (context.principal() != null) {
            throw parseError("Unsupported attribute: PRINCIPAL", context.principal());
        }

        if (context.ALL() != null) {
            throw parseError("Unsupported attribute: ALL", context);
        }

        Optional<QualifiedName> tableName = Optional.of(getQualifiedName(context.qualifiedName()));
        return new ShowGrants(
                getLocation(context),
                context.TABLE() != null,
                tableName);
    }

    @Override
    public Node visitDelete(HiveSqlParser.DeleteContext context)
    {
        return new Delete(
                getLocation(context),
                new Table(getLocation(context), getQualifiedName(context.qualifiedName())),
                visitIfPresent(context.booleanExpression(), Expression.class));
    }

    @Override
    public Node visitExplain(HiveSqlParser.ExplainContext context)
    {
        if (context.EXTENDED() != null || context.CBO() != null || context.AST() != null || context.DEPENDENCY() != null
                || context.AUTHORIZATION() != null || context.LOCKS() != null || context.VECTORIZATIONANALYZE() != null) {
            throw parseError("Only supported attribute: ANALYZE", context);
        }
        return new Explain(getLocation(context), context.ANALYZE() != null, false, (Statement) visit(context.statement()), ImmutableList.of());
    }

    @Override
    public Node visitSetSession(HiveSqlParser.SetSessionContext context)
    {
        if (context.property() != null) {
            throw parseError("Unsupported to set session property", context.property());
        }

        return new ShowSession(getLocation(context));
    }

    @Override
    public Node visitResetSession(HiveSqlParser.ResetSessionContext context)
    {
        throw parseError("Unsupported statement: Reset", context);
    }

    @Override
    public Node visitAssignmentItem(HiveSqlParser.AssignmentItemContext context)
    {
        return new AssignmentItem(getLocation(context), getQualifiedName(context.qualifiedName()), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitProperty(HiveSqlParser.PropertyContext context)
    {
        return new Property(getLocation(context), (Identifier) visit(context.identifier()), (Expression) visit(context.expression()));
    }

    // ********************** query expressions ********************
    @Override
    public Node visitQuery(HiveSqlParser.QueryContext context)
    {
        Query body = (Query) visit(context.queryNoWith());

        return new Query(
                getLocation(context),
                visitIfPresent(context.with(), With.class),
                body.getQueryBody(),
                body.getOrderBy(),
                body.getOffset(),
                body.getLimit());
    }

    @Override
    public Node visitWith(HiveSqlParser.WithContext context)
    {
        return new With(getLocation(context), context.RECURSIVE() != null, visit(context.namedQuery(), WithQuery.class));
    }

    @Override
    public Node visitNamedQuery(HiveSqlParser.NamedQueryContext context)
    {
        Optional<List<Identifier>> columns = Optional.empty();
        if (context.columnAliases() != null) {
            columns = Optional.of(visit(context.columnAliases().identifier(), Identifier.class));
        }

        return new WithQuery(
                getLocation(context),
                (Identifier) visit(context.name),
                (Query) visit(context.query()),
                columns);
    }

    @Override
    public Node visitQueryNoWith(HiveSqlParser.QueryNoWithContext context)
    {
        QueryBody term = (QueryBody) visit(context.queryTerm());

        Optional<OrderBy> orderBy = Optional.empty();
        if (context.ORDER() != null) {
            orderBy = Optional.of(new OrderBy(getLocation(context.ORDER()), visit(context.sortItem(), SortItem.class)));
        }

        if (context.clusteredBy() != null) {
            throw parseError("Unsupported attribute: CLUSTERED BY", context.clusteredBy());
        }

        if (context.distributeBy() != null) {
            throw parseError("Unsupported attribute: DISTRIBUTE BY", context.distributeBy());
        }

        if (context.sortedBy() != null) {
            throw parseError("Unsupported attribute: SORT BY", context.sortedBy());
        }

        Optional<Node> limit = Optional.empty();
        Optional<Offset> offset = Optional.empty();
        if (context.LIMIT() != null) {
            Optional<String> offsetValue = getTextIfPresent(context.offset);
            Optional<String> rowsValue = getTextIfPresent(context.rows);
            if (offsetValue.isPresent()) {
                offset = Optional.of(new Offset(offsetValue.get()));
            }

            if (rowsValue.isPresent()) {
                limit = Optional.of(new Limit(rowsValue.get()));
            }
        }

        if (term instanceof QuerySpecification) {
            QuerySpecification query = (QuerySpecification) term;
            return new Query(
                    getLocation(context),
                    Optional.empty(),
                    new QuerySpecification(
                            getLocation(context),
                            query.getSelect(),
                            query.getFrom(),
                            query.getWhere(),
                            query.getGroupBy(),
                            query.getHaving(),
                            orderBy,
                            offset,
                            limit),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        return new Query(
                getLocation(context),
                Optional.empty(),
                term,
                orderBy,
                offset,
                limit);
    }

    @Override
    public Node visitQuerySpecification(HiveSqlParser.QuerySpecificationContext context)
    {
        if (context.lateralView().size() > 0) {
            throw parseError("Unsupported statement: LATERAL VIEW", context.lateralView(0));
        }

        Optional<Relation> from = Optional.empty();
        List<SelectItem> selectItems = visit(context.selectItem(), SelectItem.class);

        List<Relation> relations = visit(context.relation(), Relation.class);
        if (!relations.isEmpty()) {
            // synthesize implicit join nodes
            Iterator<Relation> iterator = relations.iterator();
            Relation relation = iterator.next();

            while (iterator.hasNext()) {
                relation = new Join(getLocation(context), Join.Type.IMPLICIT, relation, iterator.next(), Optional.empty());
            }

            from = Optional.of(relation);
        }

        return new QuerySpecification(
                getLocation(context),
                new Select(getLocation(context.SELECT()), isDistinct(context.setQuantifier()), selectItems),
                from,
                visitIfPresent(context.where, Expression.class),
                visitIfPresent(context.groupBy(), GroupBy.class),
                visitIfPresent(context.having, Expression.class),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Override
    public Node visitGroupBy(HiveSqlParser.GroupByContext context)
    {
        return new GroupBy(getLocation(context), isDistinct(context.setQuantifier()), visit(context.groupingElement(), GroupingElement.class));
    }

    @Override
    public Node visitSingleGroupingSet(HiveSqlParser.SingleGroupingSetContext context)
    {
        return new SimpleGroupBy(getLocation(context), visit(context.groupingSet().expression(), Expression.class));
    }

    @Override
    public Node visitRollup(HiveSqlParser.RollupContext context)
    {
        return new Rollup(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitCube(HiveSqlParser.CubeContext context)
    {
        return new Cube(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitMultipleGroupingSets(HiveSqlParser.MultipleGroupingSetsContext context)
    {
        return new GroupingSets(getLocation(context), context.groupingSet().stream()
                .map(groupingSet -> visit(groupingSet.expression(), Expression.class))
                .collect(toList()));
    }

    @Override
    public Node visitSetOperation(HiveSqlParser.SetOperationContext context)
    {
        QueryBody left = (QueryBody) visit(context.left);
        QueryBody right = (QueryBody) visit(context.right);

        boolean distinct = context.setQuantifier() == null || context.setQuantifier().DISTINCT() != null;

        switch (context.operator.getType()) {
            case HiveSqlLexer.UNION:
                return new Union(getLocation(context.UNION()), ImmutableList.of(left, right), distinct);
            case HiveSqlLexer.INTERSECT:
                return new Intersect(getLocation(context.INTERSECT()), ImmutableList.of(left, right), distinct);
            case HiveSqlLexer.EXCEPT:
                return new Except(getLocation(context.EXCEPT()), left, right, distinct);
        }

        throw new IllegalArgumentException("Unsupported set operation: " + context.operator.getText());
    }

    @Override
    public Node visitSelectAll(HiveSqlParser.SelectAllContext context)
    {
        if (context.qualifiedName() != null) {
            return new AllColumns(getLocation(context), getQualifiedName(context.qualifiedName()));
        }

        return new AllColumns(getLocation(context));
    }

    @Override
    public Node visitSelectSingle(HiveSqlParser.SelectSingleContext context)
    {
        return new SingleColumn(
                getLocation(context),
                (Expression) visit(context.expression()),
                visitIfPresent(context.identifier(), Identifier.class));
    }

    @Override
    public Node visitTable(HiveSqlParser.TableContext context)
    {
        return new Table(getLocation(context), getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitSubquery(HiveSqlParser.SubqueryContext context)
    {
        return new TableSubquery(getLocation(context), (Query) visit(context.queryNoWith()));
    }

    @Override
    public Node visitInlineTable(HiveSqlParser.InlineTableContext context)
    {
        return new Values(getLocation(context), visit(context.expression(), Expression.class));
    }

    // ***************** boolean expressions ******************
    @Override
    public Node visitLogicalNot(HiveSqlParser.LogicalNotContext context)
    {
        return new NotExpression(getLocation(context), (Expression) visit(context.booleanExpression()));
    }

    @Override
    public Node visitLogicalBinary(HiveSqlParser.LogicalBinaryContext context)
    {
        return new LogicalBinaryExpression(
                getLocation(context.operator),
                getLogicalBinaryOperator(context.operator),
                (Expression) visit(context.left),
                (Expression) visit(context.right));
    }

    // *************** from clause *****************

    @Override
    public Node visitJoinRelation(HiveSqlParser.JoinRelationContext context)
    {
        Relation leftRelation = (Relation) visit(context.left);
        Relation rightRelation;
        Optional<JoinCriteria> criteria = Optional.empty();

        // Semi Join
        if (context.semiJoin() != null) {
            throw parseError("Unsupported statement: Left semi join", context.semiJoin());
        }

        // Cross join
        if (context.crossJoin() != null) {
            if (context.crossJoin().joinCriteria() != null) {
                throw parseError("Unsupported statement: Cross join must not contain join condition", context.crossJoin().joinCriteria());
            }

            rightRelation = (Relation) visit(context.crossJoin().right);
            return new Join(getLocation(context), Join.Type.CROSS, leftRelation, rightRelation, criteria);
        }

        // Inner join
        if (context.innerJoin() != null) {
            if (context.innerJoin().joinCriteria() == null) {
                throw parseError("Unsupported statement: Inner join must contain join condition", context.innerJoin());
            }

            rightRelation = (Relation) visit(context.innerJoin().right);
            criteria = Optional.of(new JoinOn((Expression) visit(context.innerJoin().joinCriteria().booleanExpression())));
            return new Join(getLocation(context), Join.Type.INNER, leftRelation, rightRelation, criteria);
        }

        // Outer join
        HiveSqlParser.OuterJoinContext outerJoinContext = context.outerJoin();

        // Get join type
        Join.Type joinType;
        if (outerJoinContext.joinType().LEFT() != null) {
            joinType = Join.Type.LEFT;
        }
        else if (outerJoinContext.joinType().RIGHT() != null) {
            joinType = Join.Type.RIGHT;
        }
        else {
            joinType = Join.Type.FULL;
        }

        // Get right relation
        rightRelation = (Relation) visit(outerJoinContext.rightRelation);

        // Get join criteria
        criteria = Optional.of(new JoinOn((Expression) visit(outerJoinContext.joinCriteria().booleanExpression())));

        return new Join(getLocation(context), joinType, leftRelation, rightRelation, criteria);
    }

    @Override
    public Node visitSampledRelation(HiveSqlParser.SampledRelationContext context)
    {
        Relation relation = (Relation) visit(context.aliasedRelation());
        if (context.TABLESAMPLE() == null) {
            return relation;
        }

        return new SampledRelation(getLocation(context), relation, SampledRelation.Type.SYSTEM, (Expression) visit(context.percentage));
    }

    @Override
    public Node visitAliasedRelation(HiveSqlParser.AliasedRelationContext context)
    {
        Relation relation = (Relation) visit(context.relationPrimary());
        if (context.identifier() == null) {
            return relation;
        }

        List<Identifier> aliases = null;
        if (context.columnAliases() != null) {
            aliases = visit(context.columnAliases().identifier(), Identifier.class);
        }

        return new AliasedRelation(getLocation(context), relation, (Identifier) visit(context.identifier()), aliases);
    }

    @Override
    public Node visitTableName(HiveSqlParser.TableNameContext context)
    {
        return new Table(getLocation(context), getQualifiedName(context.qualifiedName()));
    }

    @Override
    public Node visitSubqueryRelation(HiveSqlParser.SubqueryRelationContext context)
    {
        return new TableSubquery(getLocation(context), (Query) visit(context.query()));
    }

    // ********************* predicates *******************

    @Override
    public Node visitPredicated(HiveSqlParser.PredicatedContext context)
    {
        if (context.predicate() != null) {
            return visit(context.predicate());
        }

        return visit(context.valueExpression);
    }

    @Override
    public Node visitExpressionPredicated(HiveSqlParser.ExpressionPredicatedContext context)
    {
        throw parseError(format("Unsupported statement: %s", context.getText()), context);
    }

    @Override
    public Node visitComparison(HiveSqlParser.ComparisonContext context)
    {
        return new ComparisonExpression(
                getLocation(context.comparisonOperator()),
                getComparisonOperator(((TerminalNode) context.comparisonOperator().getChild(0)).getSymbol()),
                (Expression) visit(context.value),
                (Expression) visit(context.right));
    }

    @Override
    public Node visitDistinctFrom(HiveSqlParser.DistinctFromContext context)
    {
        Expression expression = new ComparisonExpression(
                getLocation(context),
                ComparisonExpression.Operator.IS_DISTINCT_FROM,
                (Expression) visit(context.value),
                (Expression) visit(context.right));

        if (context.NOT() != null || context.NON() != null) {
            expression = new NotExpression(getLocation(context), expression);
        }

        return expression;
    }

    @Override
    public Node visitBetween(HiveSqlParser.BetweenContext context)
    {
        Expression expression = new BetweenPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                (Expression) visit(context.lower),
                (Expression) visit(context.upper));

        if (context.NOT() != null || context.NON() != null) {
            expression = new NotExpression(getLocation(context), expression);
        }

        return expression;
    }

    @Override
    public Node visitNullPredicate(HiveSqlParser.NullPredicateContext context)
    {
        Expression child = (Expression) visit(context.value);

        if (context.NOT() == null && context.NON() == null) {
            return new IsNullPredicate(getLocation(context), child);
        }

        return new IsNotNullPredicate(getLocation(context), child);
    }

    @Override
    public Node visitLike(HiveSqlParser.LikeContext context)
    {
        Expression result = new LikePredicate(
                getLocation(context),
                (Expression) visit(context.value),
                (Expression) visit(context.pattern),
                Optional.empty());

        if (context.NOT() != null || context.NON() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitRlike(HiveSqlParser.RlikeContext context)
    {
        throw parseError("Unsupported statement: RLIKE", context);
    }

    @Override
    public Node visitRegexp(HiveSqlParser.RegexpContext context)
    {
        throw parseError("Unsupported statement: REGEXP", context);
    }

    @Override
    public Node visitInList(HiveSqlParser.InListContext context)
    {
        Expression result = new InPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                new InListExpression(getLocation(context), visit(context.expression(), Expression.class)));

        if (context.NOT() != null || context.NON() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitInSubquery(HiveSqlParser.InSubqueryContext context)
    {
        Expression result = new InPredicate(
                getLocation(context),
                (Expression) visit(context.value),
                new SubqueryExpression(getLocation(context), (Query) visit(context.query())));

        if (context.NOT() != null || context.NON() != null) {
            result = new NotExpression(getLocation(context), result);
        }

        return result;
    }

    @Override
    public Node visitExists(HiveSqlParser.ExistsContext context)
    {
        return new ExistsPredicate(getLocation(context), new SubqueryExpression(getLocation(context), (Query) visit(context.query())));
    }

    @Override
    public Node visitQuantifiedComparison(HiveSqlParser.QuantifiedComparisonContext context)
    {
        return new QuantifiedComparisonExpression(
                getLocation(context.comparisonOperator()),
                getComparisonOperator(((TerminalNode) context.comparisonOperator().getChild(0)).getSymbol()),
                getComparisonQuantifier(((TerminalNode) context.comparisonQuantifier().getChild(0)).getSymbol()),
                (Expression) visit(context.value),
                new SubqueryExpression(getLocation(context.query()), (Query) visit(context.query())));
    }

    // ************** value expressions **************

    @Override
    public Node visitArithmeticUnary(HiveSqlParser.ArithmeticUnaryContext context)
    {
        Expression child = (Expression) visit(context.valueExpression());

        switch (context.operator.getType()) {
            case HiveSqlLexer.MINUS:
                return ArithmeticUnaryExpression.negative(getLocation(context), child);
            case HiveSqlLexer.PLUS:
                return ArithmeticUnaryExpression.positive(getLocation(context), child);
            default:
                throw new UnsupportedOperationException("Unsupported sign: " + context.operator.getText());
        }
    }

    @Override
    public Node visitArithmeticBinary(HiveSqlParser.ArithmeticBinaryContext context)
    {
        return new ArithmeticBinaryExpression(
                getLocation(context.operator),
                getArithmeticBinaryOperator(context.operator),
                (Expression) visit(context.left),
                (Expression) visit(context.right));
    }

    @Override
    public Node visitArithmeticBit(HiveSqlParser.ArithmeticBitContext context)
    {
        throw parseError("Unsupported statement: Bit arithmetic", context);
    }

    @Override
    public Node visitConcatenation(HiveSqlParser.ConcatenationContext context)
    {
        return new FunctionCall(
                getLocation(context.CONCAT()),
                QualifiedName.of("concat"), ImmutableList.of(
                (Expression) visit(context.left),
                (Expression) visit(context.right)));
    }

    // ********************* primary expressions **********************

    @Override
    public Node visitParenthesizedExpression(HiveSqlParser.ParenthesizedExpressionContext context)
    {
        return visit(context.expression());
    }

    @Override
    public Node visitRowConstructor(HiveSqlParser.RowConstructorContext context)
    {
        return new Row(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitArrayConstructor(HiveSqlParser.ArrayConstructorContext context)
    {
        return new ArrayConstructor(getLocation(context), visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitCast(HiveSqlParser.CastContext context)
    {
        boolean isTryCast = context.TRY_CAST() != null;
        return new Cast(getLocation(context), (Expression) visit(context.expression()), getType(context.type()), isTryCast);
    }

    @Override
    public Node visitSpecialDateTimeFunction(HiveSqlParser.SpecialDateTimeFunctionContext context)
    {
        CurrentTime.Function function = getDateTimeFunctionType(context.name);
        return new CurrentTime(getLocation(context), function);
    }

    @Override
    public Node visitCurrentUser(HiveSqlParser.CurrentUserContext context)
    {
        return new CurrentUser(getLocation(context.CURRENT_USER()));
    }

    @Override
    public Node visitExtract(HiveSqlParser.ExtractContext context)
    {
        String fieldString = context.identifier().getText();
        Extract.Field field;
        try {
            field = Extract.Field.valueOf(fieldString.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw parseError("Invalid EXTRACT field: " + fieldString, context);
        }
        return new Extract(getLocation(context), (Expression) visit(context.valueExpression()), field);
    }

    @Override
    public Node visitSubstring(HiveSqlParser.SubstringContext context)
    {
        return new FunctionCall(getLocation(context), QualifiedName.of("substr"), visit(context.valueExpression(), Expression.class));
    }

    @Override
    public Node visitPosition(HiveSqlParser.PositionContext context)
    {
        List<Expression> arguments = Lists.reverse(visit(context.valueExpression(), Expression.class));
        return new FunctionCall(getLocation(context), QualifiedName.of("strpos"), arguments);
    }

    @Override
    public Node visitNormalize(HiveSqlParser.NormalizeContext context)
    {
        Expression str = (Expression) visit(context.valueExpression());
        String normalForm = Optional.ofNullable(context.normalForm()).map(ParserRuleContext::getText).orElse("NFC");
        return new FunctionCall(getLocation(context), QualifiedName.of("normalize"), ImmutableList.of(str, new StringLiteral(getLocation(context), normalForm)));
    }

    @Override
    public Node visitSubscript(HiveSqlParser.SubscriptContext context)
    {
        return new SubscriptExpression(getLocation(context), (Expression) visit(context.value), (Expression) visit(context.index));
    }

    @Override
    public Node visitSubqueryExpression(HiveSqlParser.SubqueryExpressionContext context)
    {
        return new SubqueryExpression(getLocation(context), (Query) visit(context.query()));
    }

    @Override
    public Node visitDereference(HiveSqlParser.DereferenceContext context)
    {
        return new DereferenceExpression(
                getLocation(context),
                (Expression) visit(context.base),
                (Identifier) visit(context.fieldName));
    }

    @Override
    public Node visitColumnReference(HiveSqlParser.ColumnReferenceContext context)
    {
        return visit(context.identifier());
    }

    @Override
    public Node visitSimpleCase(HiveSqlParser.SimpleCaseContext context)
    {
        return new SimpleCaseExpression(
                getLocation(context),
                (Expression) visit(context.valueExpression()),
                visit(context.whenClause(), WhenClause.class),
                visitIfPresent(context.elseExpression, Expression.class));
    }

    @Override
    public Node visitSearchedCase(HiveSqlParser.SearchedCaseContext context)
    {
        return new SearchedCaseExpression(
                getLocation(context),
                visit(context.whenClause(), WhenClause.class),
                visitIfPresent(context.elseExpression, Expression.class));
    }

    @Override
    public Node visitWhenClause(HiveSqlParser.WhenClauseContext context)
    {
        return new WhenClause(getLocation(context), (Expression) visit(context.condition), (Expression) visit(context.result));
    }

    @Override
    public Node visitFunctionCall(HiveSqlParser.FunctionCallContext context)
    {
        Optional<Expression> filter = Optional.empty();
        Optional<OrderBy> orderBy = Optional.empty();
        Optional<Window> window = visitIfPresent(context.over(), Window.class);
        boolean distinct = isDistinct(context.setQuantifier());

        QualifiedName name = getQualifiedName(context.qualifiedName());
        if (name.toString().equalsIgnoreCase("if")) {
            check(context.expression().size() == 3, "Illegal arguments for 'if' function", context);
            check(!window.isPresent(), "OVER not valid for 'if' function", context);
            check(!distinct, "DISTINCT not valid for 'if' function", context);

            Expression elseExpression = (Expression) visit(context.expression(2));
            return new IfExpression(
                    getLocation(context),
                    (Expression) visit(context.expression(0)),
                    (Expression) visit(context.expression(1)),
                    elseExpression);
        }

        if (name.toString().equalsIgnoreCase("nullif")) {
            check(context.expression().size() == 2, "Illegal arguments for 'nullif' function", context);
            check(!window.isPresent(), "OVER not valid for 'nullif' function", context);
            check(!distinct, "DISTINCT not valid for 'nullif' function", context);

            return new NullIfExpression(
                    getLocation(context),
                    (Expression) visit(context.expression(0)),
                    (Expression) visit(context.expression(1)));
        }

        if (name.toString().equalsIgnoreCase("coalesce")) {
            check(context.expression().size() > 0, "The 'coalesce' function must have at least one argument", context);
            check(!window.isPresent(), "OVER not valid for 'coalesce' function", context);
            check(!distinct, "DISTINCT not valid for 'coalesce' function", context);

            return new CoalesceExpression(getLocation(context), visit(context.expression(), Expression.class));
        }

        return new FunctionCall(
                Optional.of(getLocation(context)),
                getQualifiedName(context.qualifiedName()),
                window,
                filter,
                orderBy,
                distinct,
                visit(context.expression(), Expression.class));
    }

    @Override
    public Node visitOver(HiveSqlParser.OverContext context)
    {
        Optional<OrderBy> orderBy = Optional.empty();
        if (context.ORDER() != null) {
            orderBy = Optional.of(new OrderBy(getLocation(context.ORDER()), visit(context.sortItem(), SortItem.class)));
        }

        return new Window(
                getLocation(context),
                visit(context.partition, Expression.class),
                orderBy,
                visitIfPresent(context.windowFrame(), WindowFrame.class));
    }

    @Override
    public Node visitColumnDefinition(HiveSqlParser.ColumnDefinitionContext context)
    {
        Optional<String> comment = Optional.empty();
        if (context.COMMENT() != null) {
            comment = Optional.of(((StringLiteral) visit(context.string())).getValue());
        }

        if (context.columnConstraintSpecification() != null) {
            throw parseError("Unsupported attribute: Column constraint", context.columnConstraintSpecification());
        }

        return new ColumnDefinition(
                getLocation(context),
                (Identifier) visit(context.identifier()),
                getType(context.type()),
                true,
                ImmutableList.of(),
                comment);
    }

    @Override
    public Node visitSortItem(HiveSqlParser.SortItemContext context)
    {
        return new SortItem(
                getLocation(context),
                (Expression) visit(context.expression()),
                Optional.ofNullable(context.ordering)
                        .map(HiveAstBuilder::getOrderingType)
                        .orElse(SortItem.Ordering.ASCENDING),
                Optional.ofNullable(context.nullOrdering)
                        .map(HiveAstBuilder::getNullOrderingType)
                        .orElse(SortItem.NullOrdering.UNDEFINED));
    }

    @Override
    public Node visitWindowFrame(HiveSqlParser.WindowFrameContext context)
    {
        return new WindowFrame(
                getLocation(context),
                getFrameType(context.frameType),
                (FrameBound) visit(context.start),
                visitIfPresent(context.end, FrameBound.class));
    }

    @Override
    public Node visitUnboundedFrame(HiveSqlParser.UnboundedFrameContext context)
    {
        return new FrameBound(getLocation(context), getUnboundedFrameBoundType(context.boundType));
    }

    @Override
    public Node visitBoundedFrame(HiveSqlParser.BoundedFrameContext context)
    {
        return new FrameBound(getLocation(context), getBoundedFrameBoundType(context.boundType), (Expression) visit(context.expression()));
    }

    @Override
    public Node visitCurrentRowBound(HiveSqlParser.CurrentRowBoundContext context)
    {
        return new FrameBound(getLocation(context), FrameBound.Type.CURRENT_ROW);
    }

    @Override
    public Node visitGroupingOperation(HiveSqlParser.GroupingOperationContext context)
    {
        List<QualifiedName> arguments = context.qualifiedName().stream()
                .map(this::getQualifiedName)
                .collect(toList());

        return new GroupingOperation(Optional.of(getLocation(context)), arguments);
    }

    @Override
    public Node visitUnquotedIdentifier(HiveSqlParser.UnquotedIdentifierContext context)
    {
        return new Identifier(getLocation(context), context.getText(), false);
    }

    @Override
    public Node visitQuotedIdentifier(HiveSqlParser.QuotedIdentifierContext context)
    {
        String token = context.getText();
        String identifier = token.substring(1, token.length() - 1)
                .replace("\"\"", "\"");

        return new Identifier(getLocation(context), identifier, true);
    }

    @Override
    public Node visitBackQuotedIdentifier(HiveSqlParser.BackQuotedIdentifierContext context)
    {
        String token = context.getText();
        String identifier = token.substring(1, token.length() - 1)
                .replace("``", "`");

        return new Identifier(getLocation(context), identifier, true);
    }

    @Override
    public Node visitDigitIdentifier(HiveSqlParser.DigitIdentifierContext context)
    {
        throw parseError(format("Unsupported statement: %s(identifiers must not start with a digit)", context.getText()), context);
    }

    // ************** literals **************

    @Override
    public Node visitNullLiteral(HiveSqlParser.NullLiteralContext context)
    {
        return new NullLiteral(getLocation(context));
    }

    @Override
    public Node visitBasicStringLiteral(HiveSqlParser.BasicStringLiteralContext context)
    {
        return new StringLiteral(getLocation(context), unquote(context.STRING().getText()));
    }

    @Override
    public Node visitBinaryLiteral(HiveSqlParser.BinaryLiteralContext context)
    {
        String raw = context.BINARY_LITERAL().getText();
        return new BinaryLiteral(getLocation(context), unquote(raw.substring(1)));
    }

    @Override
    public Node visitTypeConstructor(HiveSqlParser.TypeConstructorContext context)
    {
        String value = ((StringLiteral) visit(context.string())).getValue();

        if (context.DOUBLE_PRECISION() != null) {
            return new GenericLiteral(getLocation(context), "DOUBLE", value);
        }

        String type = context.identifier().getText();
        if (type.equalsIgnoreCase("time")) {
            return new TimeLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("timestamp")) {
            return new TimestampLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("decimal")) {
            return new DecimalLiteral(getLocation(context), value);
        }
        if (type.equalsIgnoreCase("char")) {
            return new CharLiteral(getLocation(context), value);
        }

        return new GenericLiteral(getLocation(context), type, value);
    }

    @Override
    public Node visitIntegerLiteral(HiveSqlParser.IntegerLiteralContext context)
    {
        return new LongLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitDecimalLiteral(HiveSqlParser.DecimalLiteralContext context)
    {
        switch (convertionOptions.getDecimalLiteralTreatment()) {
            case AS_DOUBLE:
                return new DoubleLiteral(getLocation(context), context.getText());
            case AS_DECIMAL:
                return new DecimalLiteral(getLocation(context), context.getText());
            case REJECT:
                throw new ParsingException("Unexpected decimal literal: " + context.getText());
        }
        throw new AssertionError("Unreachable");
    }

    @Override
    public Node visitDoubleLiteral(HiveSqlParser.DoubleLiteralContext context)
    {
        return new DoubleLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitBooleanValue(HiveSqlParser.BooleanValueContext context)
    {
        return new BooleanLiteral(getLocation(context), context.getText());
    }

    @Override
    public Node visitInterval(HiveSqlParser.IntervalContext context)
    {
        return new IntervalLiteral(
                getLocation(context),
                context.INTEGER_VALUE().getText(),
                IntervalLiteral.Sign.POSITIVE,
                getIntervalFieldType((Token) context.intervalField().getChild(0).getPayload()),
                Optional.empty());
    }

    @Override
    public Node visitParameter(HiveSqlParser.ParameterContext context)
    {
        Parameter parameter = new Parameter(getLocation(context), parameterPosition);
        parameterPosition++;
        return parameter;
    }

    // ***************** arguments *****************

    @Override
    public Node visitQualifiedArgument(HiveSqlParser.QualifiedArgumentContext context)
    {
        return new PathElement(getLocation(context), (Identifier) visit(context.identifier(0)), (Identifier) visit(context.identifier(1)));
    }

    @Override
    public Node visitUnqualifiedArgument(HiveSqlParser.UnqualifiedArgumentContext context)
    {
        return new PathElement(getLocation(context), (Identifier) visit(context.identifier()));
    }

    @Override
    public Node visitPathSpecification(HiveSqlParser.PathSpecificationContext context)
    {
        return new PathSpecification(getLocation(context), visit(context.pathElement(), PathElement.class));
    }

    // ***************** helpers *****************

    @Override
    protected Node defaultResult()
    {
        return null;
    }

    @Override
    protected Node aggregateResult(Node aggregate, Node nextResult)
    {
        if (nextResult == null) {
            throw new UnsupportedOperationException("not yet implemented");
        }

        if (aggregate == null) {
            return nextResult;
        }

        throw new UnsupportedOperationException("not yet implemented");
    }

    private <T> Optional<T> visitIfPresent(ParserRuleContext context, Class<T> clazz)
    {
        return Optional.ofNullable(context)
                .map(this::visit)
                .map(clazz::cast);
    }

    private <T> List<T> visit(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(this::visit)
                .map(clazz::cast)
                .collect(toList());
    }

    private static String unquote(String value)
    {
        if (value.contains("\\'")) {
            throw new IllegalArgumentException("Unsupported string: " + value);
        }
        return value.substring(1, value.length() - 1);
    }

    private QualifiedName getQualifiedName(HiveSqlParser.QualifiedNameContext context)
    {
        return QualifiedName.of(visit(context.identifier(), Identifier.class));
    }

    private static boolean isDistinct(HiveSqlParser.SetQuantifierContext setQuantifier)
    {
        return setQuantifier != null && setQuantifier.DISTINCT() != null;
    }

    private static Optional<String> getTextIfPresent(ParserRuleContext context)
    {
        return Optional.ofNullable(context)
                .map(ParseTree::getText);
    }

    private static Optional<String> getTextIfPresent(Token token)
    {
        return Optional.ofNullable(token)
                .map(Token::getText);
    }

    private Optional<Identifier> getIdentifierIfPresent(ParserRuleContext context)
    {
        return Optional.ofNullable(context).map(c -> (Identifier) visit(c));
    }

    private static ArithmeticBinaryExpression.Operator getArithmeticBinaryOperator(Token operator)
    {
        switch (operator.getType()) {
            case HiveSqlLexer.PLUS:
                return ArithmeticBinaryExpression.Operator.ADD;
            case HiveSqlLexer.MINUS:
                return ArithmeticBinaryExpression.Operator.SUBTRACT;
            case HiveSqlLexer.ASTERISK:
                return ArithmeticBinaryExpression.Operator.MULTIPLY;
            case HiveSqlLexer.SLASH:
                return ArithmeticBinaryExpression.Operator.DIVIDE;
            case HiveSqlLexer.PERCENT:
                return ArithmeticBinaryExpression.Operator.MODULUS;
        }

        throw new UnsupportedOperationException("Unsupported operator: " + operator.getText());
    }

    private static ComparisonExpression.Operator getComparisonOperator(Token symbol)
    {
        switch (symbol.getType()) {
            case HiveSqlLexer.EQ:
                return ComparisonExpression.Operator.EQUAL;
            case HiveSqlLexer.NEQ:
                return ComparisonExpression.Operator.NOT_EQUAL;
            case HiveSqlLexer.LT:
                return ComparisonExpression.Operator.LESS_THAN;
            case HiveSqlLexer.LTE:
                return ComparisonExpression.Operator.LESS_THAN_OR_EQUAL;
            case HiveSqlLexer.GT:
                return ComparisonExpression.Operator.GREATER_THAN;
            case HiveSqlLexer.GTE:
                return ComparisonExpression.Operator.GREATER_THAN_OR_EQUAL;
        }

        throw new IllegalArgumentException("Unsupported operator: " + symbol.getText());
    }

    private static CurrentTime.Function getDateTimeFunctionType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.CURRENT_DATE:
                return CurrentTime.Function.DATE;
            case HiveSqlLexer.CURRENT_TIME:
                return CurrentTime.Function.TIME;
            case HiveSqlLexer.CURRENT_TIMESTAMP:
                return CurrentTime.Function.TIMESTAMP;
            case HiveSqlLexer.LOCALTIME:
                return CurrentTime.Function.LOCALTIME;
            case HiveSqlLexer.LOCALTIMESTAMP:
                return CurrentTime.Function.LOCALTIMESTAMP;
        }

        throw new IllegalArgumentException("Unsupported special function: " + token.getText());
    }

    private static IntervalLiteral.IntervalField getIntervalFieldType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.YEAR:
            case HiveSqlLexer.YEARS:
                return IntervalLiteral.IntervalField.YEAR;
            case HiveSqlLexer.MONTH:
            case HiveSqlLexer.MONTHS:
                return IntervalLiteral.IntervalField.MONTH;
            case HiveSqlLexer.DAY:
            case HiveSqlLexer.DAYS:
                return IntervalLiteral.IntervalField.DAY;
            case HiveSqlLexer.HOUR:
            case HiveSqlLexer.HOURS:
                return IntervalLiteral.IntervalField.HOUR;
            case HiveSqlLexer.MINUTE:
            case HiveSqlLexer.MINUTES:
                return IntervalLiteral.IntervalField.MINUTE;
            case HiveSqlLexer.SECOND:
            case HiveSqlLexer.SECONDS:
                return IntervalLiteral.IntervalField.SECOND;
        }

        throw new IllegalArgumentException("Unsupported interval field: " + token.getText());
    }

    private static WindowFrame.Type getFrameType(Token type)
    {
        switch (type.getType()) {
            case HiveSqlLexer.RANGE:
                return WindowFrame.Type.RANGE;
            case HiveSqlLexer.ROWS:
                return WindowFrame.Type.ROWS;
        }

        throw new IllegalArgumentException("Unsupported frame type: " + type.getText());
    }

    private static FrameBound.Type getBoundedFrameBoundType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.PRECEDING:
                return FrameBound.Type.PRECEDING;
            case HiveSqlLexer.FOLLOWING:
                return FrameBound.Type.FOLLOWING;
        }

        throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
    }

    private static FrameBound.Type getUnboundedFrameBoundType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.PRECEDING:
                return FrameBound.Type.UNBOUNDED_PRECEDING;
            case HiveSqlLexer.FOLLOWING:
                return FrameBound.Type.UNBOUNDED_FOLLOWING;
        }

        throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
    }

    private static LogicalBinaryExpression.Operator getLogicalBinaryOperator(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.AND:
                return LogicalBinaryExpression.Operator.AND;
            case HiveSqlLexer.OR:
                return LogicalBinaryExpression.Operator.OR;
        }

        throw new IllegalArgumentException("Unsupported operator: " + token.getText());
    }

    private static SortItem.NullOrdering getNullOrderingType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.FIRST:
                return SortItem.NullOrdering.FIRST;
            case HiveSqlLexer.LAST:
                return SortItem.NullOrdering.LAST;
        }

        throw new IllegalArgumentException("Unsupported ordering: " + token.getText());
    }

    private static SortItem.Ordering getOrderingType(Token token)
    {
        switch (token.getType()) {
            case HiveSqlLexer.ASC:
                return SortItem.Ordering.ASCENDING;
            case HiveSqlLexer.DESC:
                return SortItem.Ordering.DESCENDING;
        }

        throw new IllegalArgumentException("Unsupported ordering: " + token.getText());
    }

    private static QuantifiedComparisonExpression.Quantifier getComparisonQuantifier(Token symbol)
    {
        switch (symbol.getType()) {
            case HiveSqlLexer.ALL:
                return QuantifiedComparisonExpression.Quantifier.ALL;
            case HiveSqlLexer.ANY:
                return QuantifiedComparisonExpression.Quantifier.ANY;
            case HiveSqlLexer.SOME:
                return QuantifiedComparisonExpression.Quantifier.SOME;
        }

        throw new IllegalArgumentException("Unsupported quantifier: " + symbol.getText());
    }

    private String getType(HiveSqlParser.TypeContext type)
    {
        if (type.baseType() != null) {
            String signature = type.baseType().getText();
            if (type.baseType().DOUBLE_PRECISION() != null) {
                signature = "DOUBLE";
            }
            if (signature.equalsIgnoreCase("binary")) {
                signature = "varbinary";
            }
            if (!type.typeParameter().isEmpty()) {
                String typeParameterSignature = type
                        .typeParameter()
                        .stream()
                        .map(this::typeParameterToString)
                        .collect(Collectors.joining(","));
                signature += "(" + typeParameterSignature + ")";
            }

            return signature;
        }

        if (type.ARRAY() != null) {
            return "ARRAY(" + getType(type.type(0)) + ")";
        }

        if (type.MAP() != null) {
            return "MAP(" + getType(type.type(0)) + "," + getType(type.type(1)) + ")";
        }

        throw new IllegalArgumentException("Unsupported data type: " + type.getText());
    }

    private String typeParameterToString(HiveSqlParser.TypeParameterContext typeParameter)
    {
        if (typeParameter.INTEGER_VALUE() != null) {
            return typeParameter.INTEGER_VALUE().toString();
        }

        if (typeParameter.type() != null) {
            return getType(typeParameter.type());
        }

        throw new IllegalArgumentException("Illegal typeParameter: " + typeParameter.getText());
    }

    private List<Identifier> getIdentifiers(List<HiveSqlParser.IdentifierContext> identifiers)
    {
        return identifiers.stream().map(context -> (Identifier) visit(context)).collect(toList());
    }

    private List<Expression> getExpressions(List<HiveSqlParser.ExpressionContext> expressions)
    {
        return expressions.stream().map(context -> (Expression) visit(context)).collect(toList());
    }

    private List<TableElement> getTableElements(List<HiveSqlParser.TableElementContext> tableElements)
    {
        return tableElements.stream().map(context -> (TableElement) visit(context)).collect(toList());
    }

    private List<ColumnDefinition> getColumnDefinitions(List<HiveSqlParser.ColumnDefinitionContext> columnDefinitions)
    {
        return columnDefinitions.stream().map(context -> (ColumnDefinition) visit(context)).collect(toList());
    }

    private List<PrincipalSpecification> getPrincipalSpecifications(List<HiveSqlParser.PrincipalContext> principals)
    {
        return principals.stream().map(this::getPrincipalSpecification).collect(toList());
    }

    private PrincipalSpecification getPrincipalSpecification(HiveSqlParser.PrincipalContext context)
    {
        if (context instanceof HiveSqlParser.UnspecifiedPrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.UNSPECIFIED, (Identifier) visit(((HiveSqlParser.UnspecifiedPrincipalContext) context).identifier()));
        }
        else if (context instanceof HiveSqlParser.UserPrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.USER, (Identifier) visit(((HiveSqlParser.UserPrincipalContext) context).identifier()));
        }
        else if (context instanceof HiveSqlParser.RolePrincipalContext) {
            return new PrincipalSpecification(PrincipalSpecification.Type.ROLE, (Identifier) visit(((HiveSqlParser.RolePrincipalContext) context).identifier()));
        }
        else {
            throw new IllegalArgumentException("Unsupported principal: " + context);
        }
    }

    private static void check(boolean condition, String message, ParserRuleContext context)
    {
        if (!condition) {
            throw parseError(message, context);
        }
    }

    public static NodeLocation getLocation(TerminalNode terminalNode)
    {
        requireNonNull(terminalNode, "terminalNode is null");
        return getLocation(terminalNode.getSymbol());
    }

    public static NodeLocation getLocation(ParserRuleContext parserRuleContext)
    {
        requireNonNull(parserRuleContext, "parserRuleContext is null");
        return getLocation(parserRuleContext.getStart());
    }

    public static NodeLocation getLocation(Token token)
    {
        requireNonNull(token, "token is null");
        return new NodeLocation(token.getLine(), token.getCharPositionInLine());
    }

    private String getFileFormat(String hiveFileFormat)
    {
        String key = hiveFileFormat.toUpperCase(Locale.ENGLISH);
        if (HIVE_TO_HETU_FILE_FORMAT.containsKey(key)) {
            return HIVE_TO_HETU_FILE_FORMAT.get(key);
        }

        throw new IllegalArgumentException(format("Unsupported file formate: %s", hiveFileFormat));
    }

    private static ParsingException parseError(String message, ParserRuleContext context)
    {
        return new ParsingException(message, null, context.getStart().getLine(), context.getStart().getCharPositionInLine());
    }
}
