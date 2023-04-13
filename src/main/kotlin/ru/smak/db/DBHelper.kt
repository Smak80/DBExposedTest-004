package ru.smak.db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

object Departments : IntIdTable("department") {
    val name = varchar("name", 200)
    val location = varchar("location", 200)
}

class Department(
    id: EntityID<Int>,
): Entity<Int>(id){
    companion object : EntityClass<Int, Department>(Departments)

    var name by Departments.name
    var location by Departments.location

    override fun toString(): String {
        return "$id: $name -> $location"
    }
}
object Employees: IntIdTable("employee"){
    val name = varchar("name", 50)
    val job = varchar("job", 100)
    val managerId = integer("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val departmentId = integer("department_id")
        .references(Departments.id,
            onDelete = ReferenceOption.RESTRICT,
            onUpdate = ReferenceOption.CASCADE)
}

class Employee(id: EntityID<Int>) : IntEntity(id){
    companion object : IntEntityClass<Employee>(Employees)
    val name by Employees.name
    val job = Employees.varchar("job", 100)
    val managerId = Employees.integer("manager_id")
    val hireDate = Employees.date("hire_date")
    val salary = Employees.long("salary")
    val departmentId = Employees.integer("department_id")
}


object DBHelper {
    init {
        Database.connect(
            {
                DriverManager.getConnection("jdbc:mysql://localhost:3306/db004_3", "root", "")
            }
        )
    }

    fun createTables(){
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Departments, Employees, inBatch = true)
            commit()
        }
    }

    var e: Department? = null

    fun insertData(){
        transaction {
            addLogger(StdOutSqlLogger)
            e = Department.new {
                name = "Имячко"
                location = "Нахожденьице"
            }

        }
    }

    fun update() {
        transaction {
            addLogger(StdOutSqlLogger)
            e?.location = "Локация"
            e?.name = "Новое имячко"
        }
    }

    fun delete(e: Department){
        transaction {
            addLogger(StdOutSqlLogger)
            e.delete()
        }
    }
}

fun main() {
    DBHelper.createTables()
    DBHelper.insertData()
    DBHelper.update()
    DBHelper.e?.let {DBHelper.delete(it)}
    transaction {
        addLogger(StdOutSqlLogger)
        val all_deps = Department.find { Departments.location like "Бобр%" }
        all_deps.forEach { println(it) }
    }
    transaction {
        val res = Join(
            Departments, Employees,
            onColumn = Departments.id,
            otherColumn = Employees.departmentId,
            joinType = JoinType.INNER,
            additionalConstraint = {Departments.location like "Бобр%"}
        ).selectAll()
        res.forEach {
            println(it[Employees.name])
        }
    }
}